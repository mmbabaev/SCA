import java.io.PrintWriter
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.{SparkContext, SparkConf}
import scala.annotation.tailrec
import scala.io.Source

object FeaturesCreator extends App {
  val proc:Processor = new FastNLPProcessor(withDiscourse = true)

  val allLines = Source.fromFile("citation_sentiment_corpus.txt").getLines().toList

  println("total length: " + allLines.length)

  (0 until 1) foreach(i => createFile(i * 1000, i*1000 + 999))

  def createFile(from: Int, to: Int): Unit = {
    val lines = allLines.slice(from, to)

    val sentences = lines map {
      line =>
        val ar = line.split("\t")
        val doc = proc.annotateFromSentences(Seq(line.split("\t")(3)))
        val words = doc.sentences(0).words.filter { word =>
          !word.equals("''") && !word.equals(".") && !word.equals("!") && !word.equals(",") && !word.equals(":") && !word.equals("``") &&
          !word.equals("''") && !word.equals("-") && !word.equals(";") && !word.equals("''")
        }
        val s = ar(0) + "\t" + ar(1) + "\t" + ar(2) + "\t" + words.mkString(" ") + "\t" + dependenciesFromSentence(doc.sentences(0)).mkString(" ")
        s
    }
    new PrintWriter(from + "-" + to + "CITATIONS.txt") { write(sentences.mkString("\n")); close() }
  }

  def dependenciesFromSentence(sentence: Sentence): List[String] = {
    @tailrec
    def recDependencyList(acc: List[String], iterator: DirectedGraphEdgeIterator[String]): List[String] = {
      if (!iterator.hasNext) {
        return acc
      }
      val dep = iterator.next()
      val newAcc = if (dep._3.contains("punct")) {
        acc
      }
      else {
        val strDep = dep._3 + "_" + sentence.words(dep._1) + "_" + sentence.words(dep._2)
        strDep :: acc
      }

      recDependencyList(newAcc, iterator)
    }

    val result = sentence.dependencies collect {
      case dependencies: DirectedGraph[String] =>
        val iterator = new DirectedGraphEdgeIterator[String](dependencies)
        recDependencyList(List[String](), iterator)
    }

    result match {
      case Some(r: List[String]) => r
    }
  }
}

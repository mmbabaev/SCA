import java.io.PrintWriter
import Helper.CitationPatterns
import Helper.Extensions.{StringExtension, NLPProcExtension}
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.{SparkContext, SparkConf}
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import StringExtension._

object WordsAndDependenciesCreator extends App {
  val proc:Processor = new FastNLPProcessor(withDiscourse = true)

  val allLines = Source.fromFile("citation_sentiment_corpus.txt").getLines().toList

  createFeaturesFile()

  def createFeaturesFile(): Unit = {
    val lines = allLines

    val labels = lines map {
      line =>
        line.split("\t")(2)
    }

    var count = 0
    val sentencesText = lines map {
      line =>
        val ar = line.split("\t")
        var text = ar(3).replace("\\]", "]")
        text = text.replace("\\[", "(")
        text = text.replace("[", "(")
        text = text.replace("]", ")")

        val result = text.replaceCitationWithToken

        if (result == text) {
          count += 1
          println(result)
          println()
        }
        else {
//          println(ar(3))
//          println(CitationPatterns.ANY.findAllIn(ar(3)).next())
//          println()
        }
        result
        // sentiment + text
        //ar(3).replaceCitationWithToken
    }

    println("Total citations without token: " + count)

    val doc = proc.mkDocumentFromSentences(sentencesText)
    proc.tagPartsOfSpeech(doc)
    proc.lemmatize(doc)
    proc.parse(doc)
    doc.clear()

    var index = -1
    val sentences = doc.sentences map {
      sentence =>
        val words = sentence.words.filter { word =>
          !word.equals("''") && !word.equals(".") && !word.equals("!") && !word.equals(",") && !word.equals(":") && !word.equals("``") &&
          !word.equals("''") && !word.equals("-") && !word.equals(";") && !word.equals("''")
        }

        index += 1
        "id1" + "\t" + "id2" + "\t" + labels(index) + "\t" + words.mkString(" ") + "\t" + dependenciesFromSentence(sentence).mkString(" ")
    }
    new PrintWriter("sentiment_corpus.txt") { write(sentences.mkString("\n")); close() }
    println("End")
  }

  def dependenciesFromSentence(sentence: Sentence) = {
    val depList = new ArrayBuffer[String]()

    sentence.dependencies.foreach(dependencies => {
      val iterator = new DirectedGraphEdgeIterator[String](dependencies)
      while(iterator.hasNext) {
        val dep = iterator.next

        if (!dep._3.contains("punct") && !dep._3.contains("det")) {
          val words = sentence.words
          depList.append(dep._3 + "_" + words(dep._1) + "_" + words(dep._2))
        }
      }
    })

    depList.toList
  }

//  def dependenciesFromSentence(sentence: Sentence): List[String] = {
//    @tailrec
//    def recDependencyList(acc: List[String], iterator: DirectedGraphEdgeIterator[String]): List[String] = {
//      if (!iterator.hasNext) {
//        return acc
//      }
//      val dep = iterator.next()
//      val newAcc = if (dep._3.contains("punct")) {
//        acc
//      }
//      else {
//        val strDep = dep._3 + "_" + sentence.words(dep._1) + "_" + sentence.words(dep._2)
//        strDep :: acc
//      }
//
//      recDependencyList(newAcc, iterator)
//    }
//
//    val result = sentence.dependencies collect {
//      case dependencies: DirectedGraph[String] =>
//        val iterator = new DirectedGraphEdgeIterator[String](dependencies)
//        recDependencyList(List[String](), iterator)
//    }
//
//    result match {
//      case Some(r: List[String]) => r
//    }
//  }
}

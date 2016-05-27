import java.io.PrintWriter
import HelperFunctions.Extensions.{StringExtension, NlpExtension}
import HelperFunctions.Metadata
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.processors.{Sentence, Processor}
import scala.io.Source
import StringExtension._
import NlpExtension.sentenceDependencies

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
    var targetCount = 0

    val sentencesText = lines map {
      line =>
        val ar = line.split("\t")
        var text = ar(3).replace("\\]", ")")
        text = text.replace("\\[", "(")
        text = text.replace("[", "(")
        text = text.replace("]", ")")

        val targetId = ar(1)
        println(targetId)

//        val (author, year) = try{Metadata.authorAndYear(targetId)}
//        catch {
//          case e =>
//            println("error: " + targetId)
//            ("<kek>", "<kek>")
//        }
//
//        //todo: заменить имена? рефактор?
//        var result = text.replaceHarvardCitationsWithToken(author, year)
//        if (result == text) {
//          println("err: " + result)
//          targetCount += 1
//          println()
//        }
        val result = text.replaceHarvardCitationsWithToken

        if (result == text) {
          count += 1
          println(result)
          println()
        }

        result
    }

    println("Total citations without token: " + count)
    println("Total citations without target token: " + targetCount)

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
        labels(index) + "\t" + words.mkString(" ") + "\t" + sentence.dependencyList.mkString(" ")
    }
    new PrintWriter("sentiment_corpus.txt") { write(sentences.mkString("\n")); close() }
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

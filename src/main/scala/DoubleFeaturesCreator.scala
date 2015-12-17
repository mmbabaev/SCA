import java.io.PrintWriter

import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.Sentence
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Random
object DoubleFeaturesClassificator extends App {

  val fileName = "sentiment_corpus.txt"
  val citations = Citation.getCitationsFromFile(fileName)

  val sentences = citations map { c =>
    c.words
  }

  val trainDependencies: List[String] = citations flatMap { c => c.dependencies }

  val trainNGrams = List[Map[String, Double]](createTrainNGramms(1), createTrainNGramms(2), createTrainNGramms(3))

  createDoubleFeaturesFile()

  def createDoubleFeaturesFile(): Unit = {
    val pw = new PrintWriter("DoubleFeatures.txt")
    for(c <- citations) {
      pw.write(c.sentiment + "\t" + fs1FeaturesForSentence(c).toArray.mkString(" ") + "\n")
    }

    pw.close()
  }

  def fs1FeaturesForSentence(citation: Citation) = {
    val words = citation.words
    val ngrams = ((0 to 2) flatMap {i => nGramFeatures(words, i + 1, trainNGrams(i)) }).toList
    ngrams ++ dependencyFeatures(citation.dependencies, trainDependencies)
  }

  def nGramsForSentence(sentence: List[String], n: Int): Seq[String] = {
    val words = sentence.filter(
      word => !word.equals(".") && !word.equals("!") && !word.equals(",") && !word.equals("?"))

    @tailrec
    def genGram(gram: String, i: Int, end: Int): String = {
      if (i == end) gram
      else genGram(gram + " " + words(i), i + 1, end)
    }

    (0 to words.length - n) map {
      i => genGram("", i, i + n)
    }
  }

  def createTrainNGramms(n: Int): Map[String, Double] = {
    // gram -> count of this gram
    var gramMap = Map[String, Double]()
    for (sentence <- sentences) {
      // count each gram
      for(gram <- nGramsForSentence(sentence, n).toSet[String]) {
        gramMap.get(gram) match {
          case Some(count: Double) => gramMap += (gram -> (count + 1))
          case None                => gramMap += (gram -> 1)
        }
      }
    }

    // gram -> idf of gram
    val keys = gramMap.keys
    keys.foreach(key => {
      gramMap += (key -> math.log(sentences.length / gramMap(key)))
    }
    )
    gramMap
  }

  def nGramFeatures(sentence: List[String], n: Int, trainGrams: Map[String, Double]): List[Double] = {
    val grams = nGramsForSentence(sentence, n)
    var res= List[Double]()
    trainGrams.keys foreach  {
      gram =>
        if (grams.contains(gram)) {
          res = trainGrams(gram) :: res
        }
        else {
          res = 0.0 :: res
        }
    }
    res.reverse
  }

  def dependencyFeatures(dependencies: List[String], trainDependencies: List[String]) = {
    trainDependencies map {
      dep =>
        if (dependencies.contains(dep)) 1.0
        else 0.0
    }
  }
}

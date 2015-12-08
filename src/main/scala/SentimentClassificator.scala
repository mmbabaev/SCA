import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.Sentence
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

import scala.annotation.tailrec
import scala.collection.mutable

import collection.JavaConverters._
import scala.io.Source

class SentimentClassificator(fileName: String) {

  val lines = Source.fromFile(fileName).getLines().toList
  val trainSentences = (lines map {
    line =>
      val ar = line.split("\t")
      ar(3)
  }).mkString(" ")
  val proc:Processor = new CoreNLPProcessor(withDiscourse = true)
  val doc = proc.annotate(trainSentences)

  val sentiments = lines map {
    line =>
      val ar = line.split("\t")
      ar(2) match {
        case "p" => Sentiment.positive
        case "n" => Sentiment.negative
        case _ => Sentiment.objective
      }
  }

  val train1Grams = createTrainNGramms(1)
  val train2Grams = createTrainNGramms(2)
  val train3Grams = createTrainNGramms(3)
  val trainDependencies = createTrainDependencies

  val trainLabeledPoints = lines.indices map {
    i =>
      val features = fs1FeaturesForSentence(doc.sentences(i)).toArray
      LabeledPoint(sentiments(i), Vectors.dense(features))
  }

  def fs1FeaturesForSentence(s: Sentence) = {
    nGramFeatures(s, 1, train1Grams) ++
      nGramFeatures(s, 2, train2Grams) ++
      nGramFeatures(s, 3, train3Grams) ++
      dependencyFeatures(s, trainDependencies)
  }

  def nGramsForSentence(sentence: Sentence, n: Int): Seq[String] = {
    val words = sentence.words.filter(
      word => !word.equals(".") && !word.equals("!") && !word.equals(",") && !word.equals("?"))

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
    for (sentence <- doc.sentences) {
      // count each gram
      for(gram <- nGramsForSentence(sentence, n)) {
        gramMap.get(gram) match {
          case Some(count: Double) => gramMap += (gram -> (count + 1))
          case None                => gramMap += (gram -> 1)
        }
      }
    }

    gramMap.keys.foreach(key => println(key + " -> " + gramMap(key)))

    // gram -> idf of gram
    val keys = gramMap.keys
    keys.foreach(key => gramMap += (key -> math.log(doc.sentences.length / gramMap(key))))
    gramMap
  }

  def nGramFeatures(sentence: Sentence, n: Int, trainGrams: Map[String, Double]): List[Double] = {
    val grams = nGramsForSentence(sentence, n)
    println("Grams: " + grams)
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

  def dependencyFeatures(sentence: Sentence, trainDependencies: List[String]) = {
    val dependencies = dependenciesFromSentence(sentence)
    trainDependencies map {
      dep =>
        if (dependencies.contains(dep)) 1.0
        else 0.0
    }
  }

  def createTrainDependencies: List[String] = {
    val result = doc.sentences flatMap {
      sentence =>
        dependenciesFromSentence(sentence).map(el => el)
    }
    result.toList
  }

  def dependenciesFromSentence(sentence: Sentence): List[String] = {
    @tailrec
    def recDependencyList(acc: List[String], iterator: DirectedGraphEdgeIterator[String]): List[String] = {
      if (!iterator.hasNext) {
        return acc
      }
      val dep = iterator.next()
      val strDep = dep._3 + "_" + sentence.words(dep._1) + "_" + sentence.words(dep._2)
      recDependencyList(strDep :: acc, iterator)
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

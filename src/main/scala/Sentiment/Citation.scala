package Sentiment

import Helper.{SparkContextSingleton, CitationPatterns}
import edu.arizona.sista.processors.Sentence
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.regression.LabeledPoint
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.Enumeration


object Citation {
  val sc = SparkContextSingleton.getInstance()

  val trainDependencies: List[String] = sc.textFile("trainDependencies.txt").collect().toList

  var trainNGrams: List[Map[String, Double]] = ((1 to 3) map { i =>
    var map = Map[String, Double]()
    for (line <- Source.fromFile(i + "GramsTrain.txt").getLines) {
      val split = line.split("->")
      map += (split(0) -> split(1).toDouble)
    }
    map
  }).toList

  val cl = new OneVsOneSVM(sc)
}

class Citation(sentence: Sentence, val infos: Seq[String], val index: Int) {
  val dependencies = dependenciesFromSentence(sentence)

  lazy val label = getLabel

  def getLabel = Citation.cl.predict(fs1Features().toArray)

  def fs1Features() = {
    val words = sentence.words.toList
    val ngrams = ((0 to 2) flatMap {i => nGramFeatures(words, i + 1, Citation.trainNGrams(i)) }).toList
    val result = ngrams ++ dependencyFeatures(dependencies, Citation.trainDependencies)
    result
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

  def nGramsForSentence(sentence: List[String], n: Int): Seq[String] = {

    def concat(words: List[String], start: Int, end: Int) = {
      val sb = new StringBuilder()
      for (i <- start until end) {
        val space = if (i > start) " " else ""
        sb.append(space + words(i))
      }
      sb.toString().toLowerCase
    }

    val words = sentence

    val ngrams = new ArrayBuffer[String]()

    for (i <- 0 until words.length - n) {
      ngrams.append(concat(words, i, i + n))
    }
    ngrams filter { gram =>
      val ar = gram.split(" ")
      !ar.contains("a") && !ar.contains("and") && !ar.contains("to") && !ar.contains("the") && !ar.contains("of") && !ar.contains("an")
    }
  }

  def nGramFeatures(sentence: List[String], n: Int, trainGrams: Map[String, Double]): List[Double] = {
    val grams = nGramsForSentence(sentence, n)
    var res = List[Double]()
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

  def getFullText = {
    sentence.getSentenceText()
  }
}

//object Style extends Enumeration {
//  type Style = Value
//  val Harvard, Vancouver = Value
//}


package Sentiment

import Sentiment.Style.Style
import edu.arizona.sista.processors.Sentence
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import scala.annotation.tailrec
import scala.io.Source
import scala.Enumeration


object Citation {
  val cl = new SentimentClassifier()

  // todo: создать файлы для загрузки обучения
  val trainDependencies: List[String] = Source.fromFile("Sentiment/TrainDependencies.txt").getLines().toList
  val trainNGrams = List[Map[String, Double]]()
}

class Citation(sentence: Sentence, source: Paper, style: Style, startIndex: Int, endIndex: Int) {

  val dependencies = {
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

  val fs1Features = {
    val words = sentence.words.toList
    val ngrams = ((0 to 2) flatMap {i => nGramFeatures(words, i + 1, Citation.trainNGrams(i)) }).toList
    ngrams ++ dependencyFeatures(dependencies, Citation.trainDependencies)
  }

  // todo: создать классификатор
  // val sentiment = cl.predict(f1Features)

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

  def getFullText() = {
    sentence.getSentenceText()
  }

  def getCitationText() = {
    val text = getFullText()

    val regex = if (style == Style.Harvard)
      CitationPatterns.HARVARD
    else
      CitationPatterns.VANCOUVER

    regex.r.replaceAllIn(text, "")
  }
}

object Style extends Enumeration {
  type Style = Value
  val Harvard, Vancouver = Value
}


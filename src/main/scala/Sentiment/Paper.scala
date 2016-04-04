package Sentiment

import Helper.{SparkContextSingleton, CitationPatterns}
import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import Helper.Extensions.{StringExtension, NLPProcExtension}
import StringExtension._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class Paper(val text: String) {
  val proc:Processor = new FastNLPProcessor(withDiscourse = false)
  var doc = proc.mkDocument(text)

  val sentences = doc.sentences map { sentence =>
    var text = sentence.getSentenceText().replace("-LRB-", "(")
    text = text.replace("-RRB-", ")")
    text = text.replace("-LSB-", "[")
    text.replace("-RSB-", "]")
  }
  println(sentences.length)


  val citationSentencesText = sentences filter { s => isCitation(s) }
  val citationSentencesWithToken = citationSentencesText map { text =>
    println(text)
    println(text.replaceCitationWithToken)
    text.replaceCitationWithToken
  }

  val citationInfos = citationSentencesText map { text =>
    val iterator = CitationPatterns.ANY.findAllIn(text)
    val result = ArrayBuffer[String]()
    while(iterator.hasNext) {
      result.append(iterator.next())
    }
    result
  }

  doc = proc.mkDocumentFromSentences(citationSentencesWithToken)
  proc.tagPartsOfSpeech(doc)
  proc.lemmatize(doc)
  proc.parse(doc)

  val citations = for ( (cit, (infos, index)) <- doc.sentences zip
    (citationInfos zip (0 to citationInfos.length)))
    yield new Citation(cit, infos, index)


  def isCitation(sentence: Sentence) = {
    CitationPatterns.ANY.findAllIn(sentence.getSentenceText()).hasNext
  }

  def isCitation(text: String) = {
    CitationPatterns.ANY.findAllIn(text).hasNext
  }
  
  def findHarvardCitation(author: String, year: Int = 0) = {

    val result = citations filter { cit =>
      var checkConfition = false

      for (info <- cit.infos) {
        val yearCondition = if (year == 0) {
          true
        }
        else {
          info.contains(year)
        }

        if (yearCondition && info.contains(author))
          checkConfition = true
      }

      checkConfition
    }

    result
  }

  def findVancouverCitation(index: Int) = {
    val result = citations filter { cit =>
      var vancouverInfos = cit.infos filter { _.startsWith("[") }

      vancouverInfos = vancouverInfos filter { info =>
        var checkCondition = false

        val splits = info.replace(" ", "").replace("[", "").replace("]", "").split(",")
        for (s <- splits) {
          if (s == index.toString) {
            checkCondition = true
          }
          else {
            val startAndEnd = s.split("-")
            if (startAndEnd.length == 2) {
              try {
                val start = startAndEnd(0).toInt
                val end = startAndEnd(1).toInt
                if (index >= start && index <= end) {
                  checkCondition = true
                }
              }
              catch { case e => }
            }
          }
        }

        checkCondition
      }

      vancouverInfos.nonEmpty
    }



    result
  }
}

object TestPaper extends App {

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)


  val text = Source.fromFile("testPaper.txt").getLines().mkString
  val paper = new Paper(text)

  for (cit <- paper.findVancouverCitation(3)) {
    println("van: " + cit.getFullText)
    println(cit.label)
    println()
  }

  for (cit <- paper.citations) {
    println("CIT:" + cit.getFullText + "\n" + cit.label)
  }

  for (cit <- paper.findHarvardCitation("Wilson", 2005)) {
    println(cit.label)
    println(cit.getFullText)
    println()
  }
}













object TestSVM extends App {
  import org.apache.log4j.Logger
  import org.apache.log4j.Level

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[*]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("DoubleFeatures.txt")

  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    var label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }


  //val data = points
  val splits = points.randomSplit(Array(0.6, 0.4))

  val training = splits(0).cache
  val test = splits(1)

  OneVsOneSVM.load(sc, training)

  val model = new OneVsOneSVM(sc)

  var predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features.toArray)
    (prediction, label)
  }

  val metrics1 = new MulticlassMetrics(predictionAndLabels)

  sc.stop()

  println("macro F: " + metrics1.fMeasure)
  println("precision: " + metrics1.precision)
  println("recall: " + metrics1.recall)
  println()
  println("f negative: " + metrics1.fMeasure(0))
  println("f objective: " + metrics1.fMeasure(1))
  println("f positive: " + metrics1.fMeasure(2))
  println()
  println("precision negative: " + metrics1.precision(0))
  println("precision objective: " + metrics1.precision(1))
  println("precision positive: " + metrics1.precision(2))
  println("recall negative: " + metrics1.recall(0))
  println()
  println("recall negative: " + metrics1.recall(0))
  println("recall objective: " + metrics1.recall(1))
  println("recall positive: " + metrics1.recall(2))
}
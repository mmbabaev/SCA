package Sentiment

import HelperFunctions.{SparkContextSingleton, CitationPatterns}
import Sentiment.SVM.{OneVsManySVM, OneVsOneSVM}
import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import HelperFunctions.Extensions.StringExtension
import StringExtension._
import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.io.Source
import GaussianNaiveBayes._

object Paper {
  lazy val significanceModel = GaussianNaiveBayes.load("modelGNB.txt")
}

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

  var index = 0
  val citationIndexes = new ListBuffer[Int]()

  val citationSentencesText = sentences filter { s =>
    val result = isCitation(s)
    if (result) citationIndexes.append(index)
    index += 1
    result
  }
  val citationSentencesWithToken = citationSentencesText map { text =>
    text.replaceHarvardCitationsWithToken
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
    (citationInfos zip citationIndexes))
    yield new Citation(cit, infos, index, sentences(index))


  def isCitation(sentence: Sentence) = {
    CitationPatterns.ANY.findAllIn(sentence.getSentenceText()).hasNext
  }

  def isCitation(text: String) = {
    CitationPatterns.ANY.findAllIn(text).hasNext
  }

  def findCitations(id: String): Array[Citation] = {
    if (id == "") return citations

    try {
      if (id(0) == '[' && id.last == ']') {
        val index = id.substring(1, id.lastIndexOf(']'))
        findVancouverCitations(index.toInt)
      }
      else {
        val split = id.split(",")
        val year = split(1).toInt
        findHarvardCitations(split(0), year)
      }
    }
    catch {
      case e => Array[Citation]()
    }

  }

  def findHarvardCitations(author: String, year: Int = 0) = {

    val result = citations filter { cit =>
      var checkConfition = false

      for (info <- cit.infos) {
        val yearCondition = if (year == 0) {
          true
        }
        else {
          info.contains(year.toString)
        }

        val authorCondition = info.contains(author)
        if (yearCondition && authorCondition)
          checkConfition = true
      }

      checkConfition
    }

    result
  }

  def findVancouverCitations(index: Int) = {
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
              catch { case e: Throwable => }
            }
          }
        }

        checkCondition
      }

      vancouverInfos.nonEmpty
    }



    result
  }

  def isPaperSignificance(author: String, year: Int) = {
    val features = significanceFeatures(author, year).toArray
    val model = Paper.significanceModel
    model.predict(features) == 0
  }

  private val w = Source.fromFile("significance/w.txt").mkString.split(", ")
  private val d = "this, that, those, these, his, her, their, such, previous".split(", ")
  private val connectors: Seq[String] =
    ("Also, Although, Besides, But, Despite, " +
      "Even though, Furthermore, However, In addition, " +
      "In spite of, Instead, Instead of, Moreover, " +
      "Nonetheless, On the contrary, On the other hand, " +
      "Regardless of, Still, Then, Though, Whereas, While, Yet").split(", ")

  def significanceFeatures(author: String, year: Int) = {
    val significantCitations = findHarvardCitations(author, year)
    if (significantCitations.length == 0) throw new Exception("No such paper in text: " + author + "," + year)

    val formalCitationsCount = significantCitations.length // f1
    var sentencesWithAuthorsCount = 0 // f2
    var acronymsCount = 0 // f3
    var pronounsCount = 0 // f5
    var connectorsCount = 0 // f6
    var citationsListCount = 0 // f7
    var workNounsCount = 0 // f8

    val acr = "[A-Z]([A-Z]|[0-9]){2,10}"
    val acronymRegex = (acr + ".*<CIT>").r

    for (i <- significantCitations.indices) {
      val citation = significantCitations(i)
      val sentenceText = citation.getFullText

      val acronyms = new ListBuffer[String]()
      // add acronyms:
      try {
        val iterator = acronymRegex.findAllIn(sentenceText)
        while (iterator.hasNext) {
          val fullAcr = iterator.next()
          val acrText = acr.r.findAllIn(fullAcr).next()
          val window = ("(?<=" + acrText + ")(.{0,20})(?=<CIT>)").r.findAllIn(sentenceText).next()
          if (window.split(" ").length <= 4) {
            acronyms.append(acr.r.findAllIn(fullAcr).next())
          }
        }
      }
      catch { case _: Throwable =>  }

      if (citation.index + 1 != sentences.length) {
        val nextText = sentences(citation.index + 1)

        // f3 Acronyms
        if (acronyms.nonEmpty && acronyms.mkString("|").r.findAllIn(nextText).nonEmpty) {
          println(text)
          println("ACRONYMS: " + acronyms.mkString("|").r.findAllIn(nextText).next)
          println()

          acronymsCount += 1
        }
        // f5 Pronouns
        if (nextText.startsWith("He ") || nextText.startsWith("She ") || nextText.startsWith("It ")) {
          pronounsCount += 1
        }

        // f6 connectors
        for (connector <- connectors) {
          if (nextText.startsWith(connector)) {
            connectorsCount += 1
          }
        }

        // f8 Work nouns
        var hasWorkNouns = false
        for (determinator <- d) {
          for (word <- w) {
            if ((determinator + "\\s" + word).r.findAllIn(nextText).nonEmpty) {
              hasWorkNouns = true
            }
          }
        }
        if (hasWorkNouns) {
          workNounsCount += 1
        }
      }

      if (citation.hasCitationList) citationsListCount += 1
    }

    for (s <- sentences) {
      if (author.r.findAllIn(s).hasNext) sentencesWithAuthorsCount += 1
    }

    val total = 1.0

    Seq[Double](formalCitationsCount / total,
      sentencesWithAuthorsCount / total,
      acronymsCount / total,
      pronounsCount / total,
      connectorsCount / total,
      citationsListCount / total,
      workNounsCount / total)
  }
}

object TestPaper extends App {

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)


  val text = Source.fromFile("testPaper.txt").getLines().mkString
  val paper = new Paper(text)

  println("All citations:")
  for (cit <- paper.citations) {
    println("Index: " + cit.index + "\n" + "Text: " + cit.getFullText)
    println("Formal cits: " + cit.infos.mkString(", "))
    println("Sentiment: " + cit.label)
    println()
  }

  println("Citation Wilson, 2009:")
  for (cit <- paper.findHarvardCitations("Wilson", 2009)) {
    println("Index: " + cit.index + "\n" + "Text: " + cit.getFullText)
    println("Formal cits: " + cit.infos.mkString(", "))
    println("Sentiment: " + cit.label)
    println()
  }

  //  println("Vancouver citation with number 3: ")
  //  for (cit <- paper.findVancouverCitations(3)) {
  //    println("van: " + cit.getFullText)
  //    println(cit.infos.mkString(", "))
  //    println("Sentiment: " + cit.label)
  //    println()
  //  }


  println("Is Wilson, 2009 significance for this paper? \n -" + paper.isPaperSignificance("Wilson", 2009))
  println("Is Yedidia, 2003 significance for this paper? \n -" + paper.isPaperSignificance("Yedidia", 2003))
}













object TestSVM extends App {
  import org.apache.log4j.Logger
  import org.apache.log4j.Level

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)

  val sc = SparkContextSingleton.getInstance

  val lines = sc.textFile("DoubleFeatures.txt")

  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    val label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }


  val data = points
//  val l0 = points filter (_.label == 0)
//  val l1 = points filter (_.label == 1)
//  val l2 = points filter (_.label == 2)
//  val splits0 = l0.randomSplit(Array(0.6, 0.4))
//  val splits1 = l1.randomSplit(Array(0.6, 0.4))
//  val splits2 = l2.randomSplit(Array(0.6, 0.4))
//
//  val training = splits0(0) ++ splits1(0) ++ splits2(0)
//  val test = splits0(1) ++ splits1(1) ++ splits2(1)
//
// // OneVsOneSVM.load(sc, training)
  val model = new OneVsOneSVM()
 // val model = new OneVsManySVM()
  var predictionAndLabels = data.map { case LabeledPoint(label, features) =>

    val prediction = model.predict(features.toArray)
    (prediction, label)
  }

  val metrics1 = new MulticlassMetrics(predictionAndLabels)


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
  println()
  println("recall negative: " + metrics1.recall(0))
  println("recall objective: " + metrics1.recall(1))
  println("recall positive: " + metrics1.recall(2))
}

object TestOneVsMany extends App {
  val sc = SparkContextSingleton.getInstance

  val lines = sc.textFile("DoubleFeatures.txt")

  val test = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    val label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }

  val trainLines = sc.textFile("DoubleFeatures.txt")
  val train = trainLines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    val label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }
  //train.saveAsObjectFile("df")

  val data = sc.objectFile[LabeledPoint]("df")
  //  val l0 = points filter (_.label == 0)
  //  val l1 = points filter (_.label == 1)
  //  val l2 = points filter (_.label == 2)
  //  val splits0 = l0.randomSplit(Array(0.65, 0.35))
  //  val splits1 = l1.randomSplit(Array(0.65, 0.35))
  //  val splits2 = l2.randomSplit(Array(0.65, 0.35))
  //
  //  val training = splits0(0) ++ splits1(0) ++ splits2(0)
  //  val test = splits0(1) ++ splits1(1) ++ splits2(1)

  OneVsOneSVM.load(sc, data)
  val model = new OneVsOneSVM()
//  OneVsManySVM.load(sc, data)
//  val model = new OneVsManySVM()

  var predictionAndLabels = data.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics1 = new MulticlassMetrics(predictionAndLabels)



  println("f negative: " + metrics1.fMeasure(0))
  println("f objective: " + metrics1.fMeasure(1))
  println("f positive: " + metrics1.fMeasure(2))
  println()
  println("precision negative: " + metrics1.precision(0))
  println("precision objective: " + metrics1.precision(1))
  println("precision positive: " + metrics1.precision(2))
  println()
  println("recall negative: " + metrics1.recall(0))
  println("recall objective: " + metrics1.recall(1))
  println("recall positive: " + metrics1.recall(2))
  println()
  println("macro F: " + metrics1.fMeasure)
  println("precision: " + metrics1.precision)
  println("recall: " + metrics1.recall)
  println()

//  for ((p, l) <- predictionAndLabels) {
//    println(p + " " + l)
//  }

  predictionAndLabels.saveAsTextFile("svmOneVSALL")
}
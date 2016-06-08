package SentimentPackage

import HelperFunctions.{SparkContextSingleton, CitationPatterns}
import SentimentPackage.SVM.{OneVsManySVM, OneVsOneSVM}
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
      case e: Throwable => Array[Citation]()
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

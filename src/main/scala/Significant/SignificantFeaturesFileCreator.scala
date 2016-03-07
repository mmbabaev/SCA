package Significant

import java.io.{PrintWriter, File}

import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._

import scala.collection.mutable.ListBuffer
import scala.io.Source


/**
  * Created by Mihail on 29.01.16.
  */
object SignificantFeaturesFileCreator extends App {

  // Arrays for work nouns:
  val w = Source.fromFile("significance/w.txt").mkString.split(", ")
  val d = "this, that, those, these, his, her, their, such, previous".split(", ")

  val connectors: Seq[String] =
    ("Also, Although, Besides, But, Despite, " +
    "Even though, Furthermore, However, In addition, " +
      "In spite of, Instead, Instead of, Moreover, " +
      "Nonetheless, On the contrary, On the other hand, " +
      "Regardless of, Still, Then, Though, Whereas, While, Yet").split(", ")


  createFeaturesFile()

  def createFeaturesFile() = {
    val pw = new PrintWriter("significantFeatures.txt")
    val pwNorm = new PrintWriter("significantFeaturesNorm.txt")
    var allWords = Set[String]()

    val files = new File("significance/txt").listFiles().toList
    for (file <- files) {
      if (file.getName.endsWith(".txt")) {
        val name = file.getName.replaceFirst(".txt", "")
        val corpus = Source.fromFile(file).getLines() map { s =>
          val split = s.split("\t")
          val significant = if (split(1) == "Y") 0 else 1
          (split(0), significant)
        }
        val result = featuresForPapersCitingTo(name, corpus.toSeq)
        pw.write(result._1)
        pwNorm.write(result._2)
      }
    }
    pw.close()
    pwNorm.close()
  }

  def featuresForPapersCitingTo(fileName: String, corpus: Seq[(String, Int)]): (String, String) = {
    //val proc:Processor = new FastNLPProcessor(withDiscourse = true)
    val name = fileName
    val letter = name(0)
    val number = letter + name.substring(1, 3)
    var allWords = Set[String]()

    var result = ""
    var resultNorm = ""

    val browser = new Browser
    val doc = browser.parseFile("significance/html/" + name + ".html")

    val data = doc.getElementsByClass("dstPaperData")(0)
    val title = data.getElementsByClass("dstPaperTitle")(0).text()

    val authorsString = data.getElementsByClass("dstPaperAuthors")(0).text()
    val authorsArray = authorsString.split(";")
    val authors = authorsArray map (a => {
      val str = a.split(",")
      (str(0), str(1))
    })

    val dstPaper = doc.getElementsByClass("dstPaper")(0)
    //dstPaper.getElementsByClass("srcPaper").remove(0)
    var index = 0

    for (srcPaperHtml <- dstPaper.getElementsByClass("srcPaper")) {

      val srcId = corpus(index)._1
      val significant = corpus(index)._2
      index += 1
      println(name + " " + srcId)
      val paper = srcPaperHtml.child(0).child(0)

      var formalCitationsCount = 0 // f1
      var sentencesWithAuthorsCount = 0 // f2
      var acronymsCount = 0 // f3
      var pronounsCount = 0 // f5
      var connectorsCount = 0 // f6
      var citationsListCount = 0 // f7
      var workNounsCount = 0 // f8

      var sentences = ListBuffer[String]()

      val author = authors(0)._1
      val citStr = "\\([^\\)]*" + author + "[^\\)]*,? [0-9]{4}[^\\)]*\\)"
      val citRegex = citStr.r
      val otherCitationRegex = "\\([^\\)]*,? [0-9]{4}[^\\)]*\\)".r

      val acronymRegex = ("[A-Z][A-Z]+ ?[^ ]* ?[^ ]* ?[^ ]* ?[^ ]* ?[^ ]*" + citStr).r

      val lines = paper.children()

      for (i <- 0 until lines.length) {
        val line = lines(i)
        val regex = ".*title=[^\t]*:[0-9]*[\t ]".r
        val text = regex.replaceFirstIn(line.toString, "")

        sentences += text

        if (line.className().endsWith("c")) {

          // f1 Formal citations
          formalCitationsCount += 1

          // f3 Acronyms
          val acronymIterator = acronymRegex.findAllIn(text)
          var acronyms = List[String]()

          while(acronymIterator.hasNext) {
            val acronym = "[A-Z][A-Z]+".r.findAllIn(acronymIterator.next()).next()
            acronyms = acronym :: acronyms
          }

          if (i + 1 != lines.length) {
            val nextText = regex.replaceFirstIn(lines(i + 1).toString, "")

            // f3 Acronyms
            if (acronyms.nonEmpty && acronyms.mkString("|").r.findAllIn(nextText).nonEmpty) {
              println(text)
              println(acronyms.mkString("|").r.findAllIn(nextText).next)
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
                if ((determinator + "[ \t]" + word).r.findAllIn(nextText).nonEmpty) {
                  hasWorkNouns = true
                }
              }
            }
            if (hasWorkNouns) {
              workNounsCount += 1
            }
          }



          // f7 Citation list
          val allCits = otherCitationRegex.findAllIn(text).toList
          if (allCits.length > 1)
            citationsListCount += 1
          else if (allCits.nonEmpty && allCits.head.contains(";")) {
            citationsListCount += 1
          }
        }

        // f2 Authors
        if (text.contains(author)) {
          sentencesWithAuthorsCount += 1
        }
      }



//      val proc:Processor = new FastNLPProcessor(withDiscourse = false)
//
//      //val titleWords = proc.annotate(title).sentences(0).words
//
//      val doc = proc.annotateFromSentences(sentences)
//
//      val pwWords = new PrintWriter(fileName + "Words.txt")
//
//      for (sentence <- doc.sentences) {
//        for (word <- sentence.words) {
//          pwWords.write(word + "\n")
//        }
//      }
//
//      for (word <- proc.annotate(title).sentences(0).words) {
//        pwWords.write(word + "\n")
//      }
//      pwWords.close()

      val total = sentences.length.toDouble

      def featuresString(total: Double) = {
        srcId + "\t" + name + "\t" +
          formalCitationsCount / total + " " +
          sentencesWithAuthorsCount / total + " " +
          acronymsCount / total + " " +
          pronounsCount / total + " " +
          connectorsCount / total + " " +
          citationsListCount / total + " " +
          workNounsCount / total + "\t" +
          significant + "\n"
      }

      result += featuresString(1)
      resultNorm += featuresString(total)
    }



    (result, resultNorm)
  }
}

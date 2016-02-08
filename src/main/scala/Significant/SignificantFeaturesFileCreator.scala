package Significant

import java.io.{PrintWriter, File}

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

  createFeaturesFile()

  def createFeaturesFile() = {
    var result = ""
    val pw = new PrintWriter("significantFeatures.txt")

    val files = new File("significance/txt").listFiles().toList
    for (file <- files) {
      if (file.getName.endsWith(".txt")) {
        val name = file.getName.replaceFirst(".txt", "")
        val corpus = Source.fromFile(file).getLines() map { s =>
          val split = s.split("\t")
          val significant = if (split(1) == "Y") 0 else 1
          (split(0), significant)
        }
        val s = featuresForPapersCitingTo(name, corpus.toSeq)
        result += s
        pw.write(s)
      }
    }
    println(result)
    pw.close()
  }

  def featuresForPapersCitingTo(fileName: String, corpus: Seq[(String, Int)]): String = {
    //val proc:Processor = new FastNLPProcessor(withDiscourse = true)
    val name = fileName
    val letter = name(0)
    val number = letter + name.substring(1, 3)

    var result = ""

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
      //todo: f6 ???
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
          if (acronymRegex.findAllIn(text).nonEmpty) {
            acronymsCount += 1
          }


          if (i + 1 != lines.length) {
            val nextText = regex.replaceFirstIn(lines(i + 1).toString, "")

            // f5 Pronouns
            if (nextText.startsWith("He ") || nextText.startsWith("She ")) {
              pronounsCount += 1
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

          // f8 Work nouns
        }

        // f2 Authors
        if (text.contains(author)) {
          sentencesWithAuthorsCount += 1
        }


      }

      val total = 1
      result += srcId + "\t" + name + "\t" +
        formalCitationsCount / total + " " +
        sentencesWithAuthorsCount / total + " " +
        acronymsCount / total + " " +
        pronounsCount / total + " " +
        citationsListCount / total + " " +
        workNounsCount / total + "\t" +
        significant + "\n"
    }

    result
  }
}

package Significant

import java.io.{PrintWriter, File}

import SentimentPackage.Paper
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
    val pwNorm = new PrintWriter("significantFeatures.txt")

    val files = new File("significance/txt").listFiles().toList
    for (file <- files) {
      if (file.getName.endsWith(".txt")) {
        val name = file.getName.replaceFirst(".txt", "")
        val corpus = Source.fromFile(file).getLines() map { s =>
          val split = s.split("\t")
          val significant = if (split(1) == "Y") 0 else 1
          (split(0), significant)
        }
        println(file.getName)
        val result = featuresForPapersCitingTo(name, corpus.toSeq)
        pwNorm.write(result)
      }
    }
    pwNorm.close()
  }

  def featuresForPapersCitingTo(fileName: String, corpus: Seq[(String, Int)]): String = {
    //val proc:Processor = new FastNLPProcessor(withDiscourse = true)
    val name = fileName
    val letter = name(0)
    val start = name.split("-")(0)

    var year = 0

    try {
      val html = Source.fromURL(s"http://aclweb.org/anthology//$letter/$start/$fileName.bib")
      val s = html.mkString

      val yearIterator = "[0-9]{4}".r.findAllIn(s)

      if (yearIterator.hasNext) {
        year = yearIterator.next().toInt
      }
      else year = 0
    }
    catch { case _: Throwable => "year error" }

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
      val significance = corpus(index)._2
      index += 1
      println(name + " " + srcId)
      val paperHtml = srcPaperHtml.child(0).child(0)
      val lines = paperHtml.children()

      val textLines = lines map { line =>
        val regex = ".*title=[^\t]*:[0-9]*[\t ]".r
        regex.replaceFirstIn(line.toString, "")
      }

      val paper = new Paper(textLines.mkString)
      val author = authors(0)._1

      val features = paper.significanceFeatures(author, year)
      result += features.mkString(" ") + "\t" + significance + "\n"
    }

    result
  }
}

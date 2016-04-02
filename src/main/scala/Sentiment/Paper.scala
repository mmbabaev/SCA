package Sentiment

import Helper.CitationPatterns
import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import Helper.Extensions.{StringExtension, NLPProcExtension}
import StringExtension._
import org.apache.log4j.{Level, Logger}

import scala.io.Source

class Paper(val text: String) {
  val proc:Processor = new FastNLPProcessor(withDiscourse = false)
  var doc = proc.mkDocument(text)
  println(text)
  val sentences = doc.sentences
  println(sentences.length)

  var citationSentences = doc.sentences filter { s => isCitation(s) }
  val citationText = citationSentences map { c =>
    c.getSentenceText().replaceCitationWithToken
  }
  val citationInfo = citationSentences map { c =>
    CitationPatterns.ANY.findAllIn(c.getSentenceText()).next()
  }

  doc = proc.mkDocumentFromSentences(citationText)
  proc.tagPartsOfSpeech(doc)
  proc.lemmatize(doc)
  proc.parse(doc)

  println(doc.sentences.length)
  println(citationInfo.length)

  // todo: index 0
  val citations = for ( (cit, info) <- doc.sentences zip citationInfo) yield new Citation(cit, info, 0)


  def isCitation(sentence: Sentence) = {
    CitationPatterns.ANY.findAllIn(sentence.getSentenceText()).hasNext
  }
}

object TestPaper extends App {
  //todo переобучить 3 svm 0vs1 2vs1 0vs2

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)


  val text = Source.fromFile("testPaper.txt").getLines().mkString
  val paper = new Paper(text)

  for (cit <- paper.citations) {
    println(cit.getFullText)
    println(cit.getLabel)
  }
}
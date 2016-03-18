package Sentiment

import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor


class Paper(val text: String, val targetPaperId: String) {
  val proc:Processor = new FastNLPProcessor(withDiscourse = false)

  val now = System.nanoTime

  var doc = proc.mkDocument(text)

  val citationsSentences = doc.sentences.filter { sentence =>
    val text = sentence.getSentenceText()
    isHarvard(text) || isVancouver(text)
  }

  val citationTextWithoutMark = citationsSentences.map { sentence =>
    val text = sentence.getSentenceText()
    val regex = if (isVancouver(text))
      CitationPatterns.VANCOUVER
    else CitationPatterns.HARVARD

    regex.r.replaceAllIn(text, "")
  }.mkString(" ")

  val citationDoc = proc.mkDocument(citationTextWithoutMark)
  proc.tagPartsOfSpeech(citationDoc)
  proc.lemmatize(citationDoc)
  proc.parse(citationDoc)
  doc.clear()

  val citations = citationDoc.sentences.map { sentence =>
    val text = sentence.getSentenceText()
    var style = Style.Harvard
    if (isVancouver(text)) style = Style.Vancouver
    new Citation(sentence, this, style, 0, 0)
  }

  val micros = (System.nanoTime - now) / 1000000000
  println("%d seconds".format(micros))

  def isHarvard(s: String) = {
    CitationPatterns.HARVARD.r.findFirstIn(s).nonEmpty
  }

  def isVancouver(s: String) = {
    CitationPatterns.VANCOUVER.r.findFirstIn(s).nonEmpty
  }
}




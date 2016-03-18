import edu.arizona.sista.processors.Document
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor

/**
  * Created by Mihail on 18.03.16.
  */
object Extensions {
  implicit def citationString(s: String) = new {
    def replaceCitationWithToken: String = {
      CitationPatterns.ANY.replaceAll(s, "<CIT>")
    }
  }

  implicit def proc(proc: FastNLPProcessor) = new {
    def mkDocumentWithDependencies(sentences: Seq[String]) = {
      val doc = proc.mkDocumentFromSentences(sentences)
      proc.tagPartsOfSpeech(doc)
      proc.lemmatize(doc)
      proc.parse(doc)
      doc.clear()  // todo: надо ли это????????
      doc
    }
  }
}

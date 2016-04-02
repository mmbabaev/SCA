package Helper.Extensions

import edu.arizona.sista.processors.fastnlp.FastNLPProcessor

object NLPProcExtension {
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

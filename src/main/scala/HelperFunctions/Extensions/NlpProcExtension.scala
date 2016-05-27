package HelperFunctions.Extensions

import edu.arizona.sista.processors.fastnlp.FastNLPProcessor

object NlpProcExtension {
  implicit def proc(proc: FastNLPProcessor) = new {

    def mkDocumentWithDependencies(text: String) = {
      val doc = proc.mkDocument(text)
      proc.tagPartsOfSpeech(doc)
      proc.lemmatize(doc)
      proc.parse(doc)
      doc.clear() // todo: надо ли это????????
      doc
    }


  }
}
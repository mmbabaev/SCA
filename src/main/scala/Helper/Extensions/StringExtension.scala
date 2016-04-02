package Helper.Extensions

import Helper.CitationPatterns
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor

/**
  * Created by Mihail on 18.03.16.
  */
object StringExtension {
  implicit def citationString(s: String) = new {
    def replaceCitationWithToken: String = {
      CitationPatterns.ANY.replaceAllIn(s, "<CIT>")
    }
  }
}

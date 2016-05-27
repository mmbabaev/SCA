package HelperFunctions.Extensions

import HelperFunctions.CitationPatterns
import HelperFunctions.Metadata

object StringExtension {
  implicit def citationString(s: String) = new {
    def replaceHarvardCitationsWithToken: String = {
      CitationPatterns.HarvardAny.replaceAllIn(s, "<CIT>")
    }

    def replaceHarvardCitationsWithToken(author: String, year: String) = {

      val citations = CitationPatterns.HarvardAny.findAllIn(s).toList
      val targetCitations = citations.filter { cit =>
        cit.contains(author) && cit.contains(year)
      }
      var result = s
      for (c <- targetCitations) {
        result = result.replace(c, "<tCIT>")
      }
      result
    }
  }
}

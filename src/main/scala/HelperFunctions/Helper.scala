package HelperFunctions

import scala.collection.mutable.ArrayBuffer

object Helper {
  val negationTerms = ("no, not, n’t, never, neither, nor, none, nobody, nowhere, " +
    "nothing, cannot, can not, without, no one, no way").split(", ")

  def windowBasedNegation(words: Seq[String], k: Int) = {

    var i = 0
    val result = new ArrayBuffer[String]()

    while (i < words.length) {
      if (negationTerms.contains(words(i))) {
        result.append(words(i))

        for (window <- 1 to k) {
          if (i + window < words.length) {
            result.append(words(i + window) + "_neg")
          }
        }

        i += 1 + k
      }
      else {
        result.append(words(i))
        i += 1
      }
    }

    result
  }
}

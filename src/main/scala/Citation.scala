import scala.io.Source
import HelperFunctions.Helper.windowBasedNegation

object Sentiment {
  val positive = 2
  val objective = 1
  val negative = 0
}

object Citation {
  def getCitationsFromFile(fileName: String): List[Citation] = {
    val lines = Source.fromFile(fileName).getLines()

    val result = lines map { line =>
      val ar = line.split("\t")
      val sentiment = ar(0) match {
        case "p" => Sentiment.positive
        case "n" => Sentiment.negative
        case "o" => Sentiment.objective
      }
      val words = windowBasedNegation(ar(1).split(" "), 15).toList
     // val words = ar(1).split(" ").toList
      val deps = if (ar.length == 3)
        ar(2).split(" ").toList
      else
        List[String]()

      Citation(sentiment, words, deps)
    }

    result.toList
  }
}

case class Citation(sentiment: Double,
                    words: List[String],
                    dependencies: List[String]) extends Serializable {
  var features = Array[Double]()
}
import edu.stanford.nlp.ling.Sentence

import scala.io.Source

/**
 * Created by Mihail on 08.12.15.
 */
object Sentiment {
  val positive = 2
  val objective = 1
  val negative = 0
}

object Citation {
  def getCitationsFromFile(fileName: String): List[Citation] = {
    val result = Source.fromFile(fileName).getLines() map { line =>
      val ar = line.split("\t")
      val id1 = ar(0)
      val id2 = ar(1)
      val sentiment = ar(2) match {
        case "p" => Sentiment.positive
        case "n" => Sentiment.negative
        case "o" => Sentiment.objective
      }
      val words = ar(3).split(" ").toList
      val deps = ar(4).split(" ").toList
      Citation(id1, id2, sentiment, words, deps)
    }

    result.toList
  }
}

case class Citation(id1: String,
                    id2: String,
                    sentiment: Double,
                    words: List[String],
                    dependencies: List[String]) extends Serializable {
  var features = Array[Double]()
}
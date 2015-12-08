import edu.stanford.nlp.ling.Sentence

/**
 * Created by Mihail on 08.12.15.
 */
object Sentiment {
  val positive = 1
  val objective = 0
  val negative = -1
}

case class Citation(sentiment: Double, sentence: Sentence)

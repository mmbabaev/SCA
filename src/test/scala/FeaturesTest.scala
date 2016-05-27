import HelperFunctions.Metadata
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import junit.framework.TestCase
import org.junit.Assert._

class FeaturesTests extends TestCase {

  val proc = new FastNLPProcessor()

  def testNGrams {
    val sentence = ("5.3 Related works and discussion Our We found two-step model " +
      "essentially belongs to the same category as the works of <CIT> and <tCIT>").split(" ").toList
    println(DoubleFeaturesClassificator.nGramsForSentence(sentence, 1))
    println(DoubleFeaturesClassificator.nGramsForSentence(sentence, 2))
    println(DoubleFeaturesClassificator.nGramsForSentence(sentence, 3))
  }
}
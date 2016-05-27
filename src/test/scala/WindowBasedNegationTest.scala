import HelperFunctions.Helper.windowBasedNegation
import junit.framework.TestCase
import org.junit.Assert._

class WindowBasedNegationTest extends TestCase {

  def testWindowBasedNegation() = {
    val words = "I never heard about this rule".split(" ")
    val expected = "I never heard_neg about_neg this rule".split(" ")
    val result = windowBasedNegation(words, 2)
    for (i <- words.indices) {
      assertEquals(expected(i), result(i))
    }
  }

  def testLastWBN() = {
    val words = "I never heard about this rule not".split(" ")
    val expected = "I never heard_neg about this rule not".split(" ")
    val result = windowBasedNegation(words, 1)
    for (i <- words.indices) {
      assertEquals(expected(i), result(i))
    }
  }

  def testPreLastWBN() = {
    val words = "I heard about this rule nor another one".split(" ")
    val expected = "I heard about this rule nor another_neg one_neg".split(" ")
    val result = windowBasedNegation(words, 3)
    for (i <- words.indices) {
      assertEquals(expected(i), result(i))
    }
  }
}
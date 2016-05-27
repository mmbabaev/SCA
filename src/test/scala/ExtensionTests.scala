import HelperFunctions.Metadata
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import junit.framework.TestCase
import org.junit.Assert._
import HelperFunctions.Extensions.StringExtension.citationString
import HelperFunctions.Extensions.NlpExtension.sentenceDependencies

class ExtensionTests extends TestCase {

  val proc = new FastNLPProcessor()

  def testCitationTokens {
    var test = "Dasgupta and Ng (2007) improves over (Creutz, 2003) by suggesting a simpler approach."
    val id = "N07-1020"
    val (author, year) = Metadata.authorAndYear(id)
    test = test.replaceHarvardCitationsWithToken(author, year)
    test = test.replaceHarvardCitationsWithToken
    assertEquals(test, "<tCIT> improves over <CIT> by suggesting a simpler approach.")
  }

  def testSentenceDependencies {
    val text = "For each training data size, we report the size of the resulting language model," +
      " the fraction of 5-grams from the test data that is present in the language model," +
      " and the BLEU score (Papineni et al. , 2002) obtained by the machine translation system."
    val doc = proc.mkDocument(text)
    proc.tagPartsOfSpeech(doc)
    proc.lemmatize(doc)
    proc.parse(doc)
    doc.clear()

    val deps = "List(nn_size_training, nn_size_data," +
      " prep_for_report_size, nsubj_report_we, dobj_report_size," +
      " npadvmod_report_fraction, npadvmod_report_score, prep_of_size_model," +
      " amod_model_resulting, nn_model_language, prep_of_fraction_5-grams," +
      " prep_from_fraction_data, conj_and_fraction_score, nn_data_test, rcmod_data_present," +
      " nsubj_present_that, cop_present_is, prep_in_present_model, nn_model_language," +
      " nn_score_BLEU, dep_score_Papineni, vmod_score_obtained, nn_Papineni_et, dep_Papineni_al.," +
      " amod_Papineni_2002, agent_obtained_system, nn_system_machine, nn_system_translation)"


    val sentence = doc.sentences(0)
    assertEquals(sentence.dependencyList.toString, deps)
  }
}
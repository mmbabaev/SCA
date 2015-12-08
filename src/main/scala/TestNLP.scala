import edu.arizona.sista.processors.Processor
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.struct.DirectedGraphEdgeIterator

/**
 * Created by Mihail on 02.12.15.
 */
object TestNLP extends App {
  val text = "John Smith went to China. He visited Beijing, on January 10th, 2013. He leaves China on January."
  val c = new SentimentClassificator(text)
  val trainGrams = c.createTrainNGramms(2)

  println("KEYS:")
  trainGrams.keys.foreach(key => println(key))
  println("END KEYS")

  val proc:Processor = new CoreNLPProcessor(withDiscourse = true)

  // the actual work is done here
  val doc = proc.annotate(text)

  println("BiGram features: ")
  for(sentence <- doc.sentences) {
    println()

    val features = c.nGramFeatures(sentence, 2, trainGrams)
    println(features)
    var s = ""
    var i = 0
    for(gramKey <- trainGrams.keys) {
      s += gramKey + "-" + trainGrams(gramKey) + "\t" + features(i) + "\n"
      i += 1
    }

    println(sentence.words.mkString(" ") + "\n" + s)
  }

  println("Train Dependency features:")
  val trainDependencies = c.createTrainDependencies
  trainDependencies.foreach(d => println(d))

  println("features: ")
  doc.sentences.foreach(s => {
    println(s.words.mkString(" "))
    val features = c.dependencyFeatures(s, trainDependencies)
    var i = 0
    for(dep <- trainDependencies) {
      print(dep + "\t" + features(i) + "\n")
      i += 1
    }
  })
}
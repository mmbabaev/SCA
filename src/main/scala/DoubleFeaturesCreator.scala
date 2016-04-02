import java.io.PrintWriter
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.json.JSONObject


object DoubleFeaturesClassificator extends App {

  val fileName = "sentiment_corpus.txt"
  val citations = Citation.getCitationsFromFile(fileName)

  val sentences = citations map { c =>
    c.words
  }

  val trainNGrams = List[Map[String, Double]](createTrainNGramms(1), createTrainNGramms(2), createTrainNGramms(3))

  for (n <- 1 to 3) {
    val pw = new PrintWriter(n + "GramsTrain.txt")

    for ((key, value) <- trainNGrams(n - 1)) {
      pw.write(key + "->" + value + "\n")
    }

    pw.close()
  }

  val trainDependencies: List[String] = (citations flatMap { c => c.dependencies }).distinct

  var pw = new PrintWriter("trainDependencies.txt")
  for (dep <- trainDependencies) {
    pw.write(dep + "\n")
  }
  pw.close()





  createDoubleFeaturesFile()

//  def createDoubleFeaturesFile(): Unit = {
//    val pw = new PrintWriter("DoubleFeatures.txt")
//    var i = 0
//    var result = ""
//
//    for(c <- citations) {
//      println(i)
//   	  i += 1
//      result += c.sentiment + "\t"
//      for (feature <- fs1FeaturesForSentence(c).toArray) {
//      	if (feature == 0) {
//      	  result += "0 "
//      	}
//      	else {
//        	result += feature + " "
//      	}
//      }
//      result += "\n"
//    }
//
//    pw.write(result)
//    pw.close()
//  }

  def createDoubleFeaturesFile(): Unit = {
    val pw = new PrintWriter("DoubleFeatures.txt")
    var i = 0

    for(c <- citations) {

      println(i)
      i += 1
      pw.write(c.sentiment + "\t")
      for (feature <- fs1FeaturesForSentence(c).toArray) {
        if (feature == 0) {
          pw.write("0 ")
        }
        else {
          pw.write(feature + " ")
        }
      }
      pw.write("\n")
    }

    pw.close()
  }


  def fs1FeaturesForSentence(citation: Citation) = {
    val words = citation.words
    val ngrams = ((0 to 2) flatMap {i => nGramFeatures(words, i + 1, trainNGrams(i)) }).toList
    val result = ngrams ++ dependencyFeatures(citation.dependencies, trainDependencies)
    result
  }

  def nGramsForSentence(sentence: List[String], n: Int): Seq[String] = {

    def concat(words: List[String], start: Int, end: Int) = {
      val sb = new StringBuilder()
      for (i <- start until end) {
        val space = if (i > start) " " else ""
        sb.append(space + words(i))
      }
      sb.toString().toLowerCase
    }

    val words = sentence

    val ngrams = new ArrayBuffer[String]()

    for (i <- 0 until words.length - n) {
      ngrams.append(concat(words, i, i + n))
    }
    ngrams filter { gram =>
      val ar = gram.split(" ")
      !ar.contains("a") && !ar.contains("and") && !ar.contains("to") && !ar.contains("the") && !ar.contains("of") && !ar.contains("an")
    }
  }

//  def nGramsForSentence(sentence: List[String], n: Int): Seq[String] = {
//    val words = sentence.filter(
//      word => !word.equals(".") && !word.equals("!") && !word.equals(",") && !word.equals("?"))
//
//    @tailrec
//    def genGram(gram: String, i: Int, end: Int): String = {
//      if (i == end) gram
//      else genGram(gram + " " + words(i), i + 1, end)
//    }
//
//    (0 to words.length - n) map {
//      i => genGram("", i, i + n)
//    }
//  }

  def createTrainNGramms(n: Int): Map[String, Double] = {
    // gram -> count of this gram
    var gramMap = Map[String, Double]()
    for (sentence <- sentences) {
      // count each gram
      for(gram <- nGramsForSentence(sentence, n).toSet[String]) {
        gramMap.get(gram) match {
          case Some(count: Double) => gramMap += (gram -> (count + 1))
          case None                => gramMap += (gram -> 1)
        }
      }
    }

    // gram -> idf of gram
    val keys = gramMap.keys
    keys.foreach(key => {
      gramMap += (key -> math.log(sentences.length / gramMap(key)) / math.log(2))
    })
    gramMap
  }

  def nGramFeatures(sentence: List[String], n: Int, trainGrams: Map[String, Double]): List[Double] = {
    val grams = nGramsForSentence(sentence, n)
    var res = List[Double]()
    trainGrams.keys foreach  {
      gram =>
        if (grams.contains(gram)) {
          res = trainGrams(gram) :: res
        }
        else {
          res = 0.0 :: res
        }
    }
    res.reverse
  }

  def dependencyFeatures(dependencies: List[String], trainDependencies: List[String]) = {
    trainDependencies map {
      dep =>
        if (dependencies.contains(dep)) 1.0
        else 0.0
    }
  }
}

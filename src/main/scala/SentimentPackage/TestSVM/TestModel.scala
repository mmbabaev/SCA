package SentimentPackage.TestSVM

import HelperFunctions.{IdfModel, SparkContextSingleton}
import SentimentPackage.SVM.{OneVsOneSVM, OneVsManySVM}
import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.classification.NaiveBayes

import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.feature.{IDF, HashingTF}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ArrayBuffer


object TestModel extends App {

  val fileName = "sentiment_corpus.txt"
  val citations = Citation.getCitationsFromFile(fileName)
  println("COUNT: " + citations.length)
  val sc = SparkContextSingleton.getInstance

  val negCits = citations filter { _.sentiment == 0 }
  val obCits = citations filter { _.sentiment == 1 }
  val posCits = citations filter { _.sentiment == 2 }

  val trainFreq = 0.65
  val testFreq = 0.35

  val splitsNeg = sc.parallelize(negCits).randomSplit(Array(trainFreq, testFreq))
  val splitsOb = sc.parallelize(obCits).randomSplit(Array(trainFreq, testFreq))
  val splitsPos = sc.parallelize(posCits).randomSplit(Array(trainFreq, testFreq))

  val train = splitsNeg(0) ++ splitsOb(0) ++ splitsPos(0)
  val test = splitsNeg(1) ++ splitsOb(1) ++ splitsPos(1)

  val trainDocs = citationsToDocs(train)
  val testDocs = citationsToDocs(test)

  val trainLabels = train map { _.sentiment }
  val testLabels = test map { _.sentiment }

  val idfModel = IdfModel(trainDocs)
  val idfs = idfModel.transform(trainDocs)

  val zipped = trainLabels.zip(idfs)

  val labeledPoints = zipped.map { case (label, vector) => LabeledPoint(label, vector) }
  labeledPoints.cache()

  //val model = NaiveBayes.train(labeledPoints)

  OneVsOneSVM.load(sc, labeledPoints)
  val model = new OneVsOneSVM(sc)



  val testVectors = idfModel.transform(testDocs)
  val zippedTest = testLabels.zip(testVectors)
  val testPoints = zippedTest map { case (label, vector) => LabeledPoint(label, vector) }
  testPoints.cache()

 // val predictionAndLabels = labeledPoints.map { case LabeledPoint(label, features) =>
  val predictionAndLabels = testPoints.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics1 = new MulticlassMetrics(predictionAndLabels)

  val p0and0 = predictionAndLabels filter { p => p._1 == 0 && p._2 == 0 }
  val p0and1 = predictionAndLabels filter { p => p._1 == 0 && p._2 == 1 }
  val p0and2 = predictionAndLabels filter { p => p._1 == 0 && p._2 == 2 }
  val p1and0 = predictionAndLabels filter { p => p._1 == 1 && p._2 == 0 }
  val p1and1 = predictionAndLabels filter { p => p._1 == 1 && p._2 == 1 }
  val p1and2 = predictionAndLabels filter { p => p._1 == 1 && p._2 == 2 }
  val p2and0 = predictionAndLabels filter { p => p._1 == 2 && p._2 == 0 }
  val p2and1 = predictionAndLabels filter { p => p._1 == 2 && p._2 == 1 }
  val p2and2 = predictionAndLabels filter { p => p._1 == 2 && p._2 == 2 }



  println("F: " + metrics1.fMeasure)
  println("micro f" + metrics1.weightedFMeasure)
  val f0 = metrics1.fMeasure(0)
  val f1 = metrics1.fMeasure(1)
  val f2 = metrics1.fMeasure(2)

  println("macro f" + (f0 + f1 + f2) / 3.0)
  println("F0: " + f0)
  println("F1: " + f1)
  println("F2: " + f2)
  println("Precision: " + metrics1.precision)
  println("p0: " + metrics1.precision(0))
  println("p1: " + metrics1.precision(1))
  println("p2: " + metrics1.precision(2))
  println("Recall: " + metrics1.recall)
  println("r0: " + metrics1.recall(0))
  println("r1:" + metrics1.recall(1))
  println("r2: " + metrics1.recall(2))

  println("p0and0 " + p0and0.count())
  println("p0and1 " + p0and1.count())
  println("p0and2 " + p0and2.count())
  println("p1and0 " + p1and0.count())
  println("p1and1 " + p1and1.count())
  println("p1and2 " + p1and2.count())
  println("p2and0 " + p2and0.count())
  println("p2and1 " + p2and1.count())
 println("p2and2 " + p2and2.count())

  def citationsToDocs(citations: RDD[Citation]) = {
    citations map { c =>
      val grams1 = nGramsForSentence(c.words, 1)
      val grams2 = nGramsForSentence(c.words, 2)
      val grams3 = nGramsForSentence(c.words, 3)
      val deps = c.dependencies.toSeq

      grams1 ++ grams2 ++ grams3 ++ deps
       //grams1 ++ grams2 ++ grams3
    }
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

    for (i <- 0 until words.length - n + 1) {
      ngrams.append(concat(words, i, i + n))
    }
    ngrams
  }
}


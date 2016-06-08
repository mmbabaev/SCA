package SentimentPackage.TestSVM

import HelperFunctions.{IdfModel, SparkContextSingleton}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.feature.{IDF, HashingTF}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils.kFold
import org.apache.spark.mllib.classification.{SVMWithSGD, NaiveBayes}
import org.apache.spark.mllib.linalg

object TestOneVsOne extends App {
  val fileName = "sentiment_corpus.txt"

  val citations = Citation.getCitationsFromFile(fileName)
  val sc = SparkContextSingleton.getInstance

  val data = sc.parallelize(citations)

  val C = Seq(0.001, 0.01, 0.1, 0.0, 1.0, 10.0)

  val regFmeasure = collection.mutable.HashMap[Double, Double]()

  for (c <- C) {
    val steps = 10
    val results = (1 to steps) map { pow =>

      val targetLabel1 = 1
      val targetLabel2 = 2
      val labelTargetPoints = (data.filter(_.sentiment == targetLabel1) map {
        citation =>
          Citation(1.0, citation.words, citation.dependencies)
      }).randomSplit(Array(0.6, 0.4), pow)

      val labelOtherPoints = (data.filter(_.sentiment == targetLabel2) map {
        cit =>
          Citation(0.0, cit.words, cit.dependencies)
      }).randomSplit(Array(0.6, 0.4), pow)

      val train = labelTargetPoints(0) ++ labelOtherPoints(0)
      val test = labelTargetPoints(1) ++ labelOtherPoints(1)


      val trainDocs = TestModel.citationsToDocs(train)
      val testDocs = TestModel.citationsToDocs(test)

      val trainLabels = train map {
        _.sentiment
      }

      val testLabels = test map {
        _.sentiment
      }

      val idfModel = IdfModel(trainDocs)
      val idfs = idfModel.transform(trainDocs)

      val zipped = trainLabels.zip(idfs)

      val labeledPoints = zipped.map { case (label, vector) => LabeledPoint(label, vector) }
      labeledPoints.cache()

      val model = SVMWithSGD.train(labeledPoints, 10, 1.0, c)

      val testVectors = idfModel.transform(testDocs)
      val zippedTest = testLabels.zip(testVectors)
      val testPoints = zippedTest map { case (label, vector) => LabeledPoint(label, vector) }
      testPoints.cache()

      val predictionAndLabels = testPoints.map { case LabeledPoint(label, features) =>
        val prediction = model.predict(features)
        (prediction, label)
      }

      val metrics = new MulticlassMetrics(predictionAndLabels)
      (metrics.fMeasure(0) + metrics.fMeasure(1)) / 2.0
    }

    val f = results.sum / (steps * 1.0)
    println("C = " + c + "\t macro F = " + f)
    regFmeasure.put(c, f)
  }

  sc.stop()

  for ((c, f) <- regFmeasure) {
    println("C = " + c + "\t F = " + f)
  }
}
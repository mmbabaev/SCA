import java.io.PrintWriter

import org.apache.spark.mllib.classification.{SVMWithSGD, SVMModel, ClassificationModel, NaiveBayes}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

import scala.io.Source


object SparkMain extends App {
  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[4]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("DoubleFeatures.txt")
  
  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    LabeledPoint(ar(0).toDouble, Vectors.dense(features))
  }

  val splits = points.randomSplit(Array(0.6, 0.4), seed = 11L)

  val training = splits(0).cache()
  val test = splits(1)

  val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

  val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics = new MulticlassMetrics(predictionAndLabels)

  println("macro F: " + metrics.fMeasure)
  println("micro F: " + microFMetric())

  model.save(sc, "model")

  def microFMetric(): Double = {
    val tpSum = truePositiveSum
    val fpSum = falsePositiveSum
    val fnSum = falseNegativeSumForClass(0) + falseNegativeSumForClass(1) + falseNegativeSumForClass(2)

    val avrPrecision = tpSum / (tpSum + fpSum)
    val avrRecall = tpSum / (tpSum + fnSum)
    (avrPrecision + avrRecall) / 2
  }

  def truePositiveSum: Double = {
    predictionAndLabels
      .map { case (prediction, label) =>
      (label, if (label == prediction) 1 else 0)
    }.reduceByKey(_ + _)
      .collectAsMap().values.sum
  }
  def falsePositiveSum: Double = {
    predictionAndLabels
      .map { case (prediction, label) =>
      (prediction, if (prediction != label) 1 else 0)
    }.reduceByKey(_ + _)
      .collectAsMap().values.sum
  }
  def falseNegativeSumForClass(label: Double): Double = {
    predictionAndLabels
      .map { case (prediction, l) =>
      (label, if (l == label && prediction != l) 1 else 0)
    }.reduceByKey(_ + _)
      .collectAsMap()(label)
  }
}

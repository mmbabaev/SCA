import java.io.PrintWriter

import GaussianNaiveBayes.GaussianNaiveBayes
import org.apache.spark.mllib.classification.{SVMWithSGD, SVMModel, ClassificationModel, NaiveBayes}
import org.apache.spark.mllib.evaluation.{BinaryClassificationMetrics, MulticlassMetrics}
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkContext, SparkConf}

import scala.io.Source

object SentimentMain extends App {
  import org.apache.log4j.Logger
  import org.apache.log4j.Level

  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[4]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("DoubleFeatures.txt")

  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    var label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }

  val scaler = new StandardScaler(withMean = true, withStd = true).fit(points.map(x => x.features))
  val data = points map { p => LabeledPoint(p.label, scaler.transform(p.features)) }
  //val data = points
  val splits = data.randomSplit(Array(0.6, 0.4))

  //val training = splits(0).persist(StorageLevel.MEMORY_AND_DISK_2)
  //  //val test = splits(1).persist(StorageLevel.MEMORY_AND_DISK)
  val training = splits(0).cache
  val test = splits(1)


  val model = SVMWithSGD.train(training,  5)
  model.clearThreshold()
  //val model = NaiveBayes.train(data, lambda = 1.0, modelType = "multinomial")
  //val model = new GaussianNaiveBayes(training, Array(0, 1, 2))

  var predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)

    (prediction, label)
  }

  predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    var prediction = 1
    if (model.predict(features) > 0)
      prediction = 0

    (prediction, label)
  }

  val metrics1 = new MulticlassMetrics(predictionAndLabels)

  println("macro F: " + metrics1.fMeasure)
  println("F: " + metrics1.fMeasure(0))
  println("F: " + metrics1.fMeasure(1))
  println("F: " + metrics1.precision)
  println("F: " + metrics1.recall)
  println("F: " + metrics1.precision(0))
  println("F: " + metrics1.precision(1))
  println("F: " + metrics1.precision)
  println("F: " + metrics1.recall(0))
  println("F: " + metrics1.recall(1))
  println("F: " + metrics1.recall)


  model.save(sc, "modelNB")

  predictionAndLabels foreach {p =>
    println(p._1 + " " + p._2)
  }

  // println("micro F: " + microFMetric())

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

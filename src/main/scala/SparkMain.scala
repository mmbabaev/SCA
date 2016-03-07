import java.io.PrintWriter

import org.apache.spark.mllib.classification.{SVMWithSGD, SVMModel, ClassificationModel, NaiveBayes}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkContext, SparkConf}

import scala.io.Source

object SqlCreator {
  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[1]")
  val sc = new SparkContext(conf)
  val sqlContext = new org.apache.spark.sql.SQLContext(sc)
  import sqlContext.implicits._

  case class Observer(sentiment: Double, features: Seq[Double])

  val observers = sc.textFile("DoubleFeatures.txt").map(_.split("\t")).map(p => Observer(p(0).toInt, p(1).map(_.toDouble))).toDF()
  observers.registerTempTable("observers")
  observers.write.save("observers.parquet")
}

object SparkMain extends App {
  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[1]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("DoubleFeatures.txt")

  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    LabeledPoint(ar(0).toDouble, Vectors.dense(features))
  }

  val data = points

  val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)

//  val training = splits(0).persist(StorageLevel.MEMORY_AND_DISK_2)
//  val test = splits(1).persist(StorageLevel.MEMORY_AND_DISK)
  val training = splits(0)
  val test = splits(1)

  val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

  val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics = new MulticlassMetrics(predictionAndLabels)

  println("macro F: " + metrics.fMeasure)
  println("micro F: " + microFMetric())

  model.save(sc, "model123")

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

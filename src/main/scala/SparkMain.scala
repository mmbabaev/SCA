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

  val lines = sc.textFile("Features.txt")

  val points = lines map {line =>
    val ar = line.split("\t")
    val features=  ar(1).split(" ") map(i => i.toDouble)
    LabeledPoint(ar(0).toDouble, Vectors.dense(features))
  }

  println(points.count())

  val splits = points.randomSplit(Array(0.6, 0.4), seed = 11L)
  val training = splits(0).cache()
  val test = splits(1)


  val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

  val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics = new MulticlassMetrics(predictionAndLabels)
  print("F: " + metrics.fMeasure)
}

package Sentiment

import java.io.PrintWriter

import org.apache.log4j.{Level, Logger}
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

/**
  * Created by Mihail on 31.03.16.
  */
object Test extends App {
  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[*]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("DoubleFeatures.txt")
  val points = lines map {line =>
    val ar = line.split("\t")
    val features =  ar(1).split(" ") map(i => i.toDouble)
    val label = ar(0).toDouble
    LabeledPoint(label, Vectors.dense(features))
  }

  val scaler = new StandardScaler(withMean = true, withStd = true).fit(points.map(x => x.features))

  var data = points map { p => LabeledPoint(p.label, scaler.transform(p.features)) }

  //todo serialize scaler
  val means = scaler.mean.toArray
  val stds = scaler.std.toArray

  OneVsOneSVM.load(sc, data)
//  var pw = new PrintWriter("means.txt")
//  for (m <- means) {
//    pw.write(m + "\n")
//  }
//  pw.close()
//
//  pw = new PrintWriter("stds.txt")
//  for (s <- stds) {
//    pw.write(s + "\n")
//  }
//  pw.close()
  //val model = new OneVsOneSVM(sc, data)
}

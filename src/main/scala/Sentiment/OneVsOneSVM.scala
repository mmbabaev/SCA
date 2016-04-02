package Sentiment

import java.io.PrintWriter


import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.linalg.Vectors

import org.apache.spark.mllib.feature.StandardScalerModel

object OneVsOneSVM {
  def load(sc: SparkContext, data: RDD[LabeledPoint]): Unit = {
    var points = data filter(p => p.label != 2)
    val svm0vs1 = SVMWithSGD.train(points, 10, 2, 0.01)
    svm0vs1.save(sc, "svm0vs1")

    points = data filter(p => p.label != 0)
    points = points map (p =>
      if (p.label == 2) {
        LabeledPoint(0, p.features)
      }
      else {
        p
      })

    val svm2vs1 = SVMWithSGD.train(points, 10, 2, 0.01)
    svm2vs1.save(sc, "svm2vs1")

    points = data filter(p => p.label != 1)
    points = points map { p =>
      if (p.label == 2) {
        LabeledPoint(1, p.features)
      }
      else {
        p
      }
    }
    val svm0vs2 = SVMWithSGD.train(points, 10, 2, 0.01)
    svm0vs2.save(sc, "svm0vs2")
  }
}

class OneVsOneSVM(sc: SparkContext) {
  val svm0vs1 = SVMModel.load(sc, "svm0vs1")
  val svm0vs2 = SVMModel.load(sc, "svm0vs2")
  val svm2vs1 = SVMModel.load(sc, "svm2vs1")

  svm0vs1.clearThreshold()
  svm0vs2.clearThreshold()
  svm2vs1.clearThreshold()

  val means = Vectors.dense((sc.textFile("means.txt") map { line => line.toDouble }).collect)
  val stds  = Vectors.dense((sc.textFile("stds.txt")  map { line => line.toDouble }).collect)
  val scalerModel = new StandardScalerModel(stds, means)

  def predict(features: Array[Double]): Double = {

    val scaledFeatures = scalerModel.transform(Vectors.dense(features))

    val prediction0vs1 = svm0vs1.predict(scaledFeatures)
    val prediction0vs2 = svm0vs2.predict(scaledFeatures)
    val prediction2vs1 = svm2vs1.predict(scaledFeatures)

    val label0Score = -prediction0vs1 - prediction0vs2
    val label1Score = prediction0vs1 + prediction2vs1
    val label2Score = prediction0vs2 - prediction2vs1

    if (label0Score > label1Score) {
      if (label0Score > label2Score) 0 else 2
    }
    else {
      if (label2Score > label1Score) 2 else 1
    }
  }
}



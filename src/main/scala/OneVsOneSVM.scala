import java.io.File

import HelperFunctions.SparkContextSingleton
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{ClassificationModel, SVMModel, SVMWithSGD}
import org.apache.spark.mllib.feature.{StandardScaler, StandardScalerModel}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.Saveable
import org.apache.spark.rdd.RDD

object OneVsOneSVM {
  def load(sc: SparkContext, data: RDD[LabeledPoint], sgdSteps: Int = 10, a: Double = 1.2, c: Double = 0.1): Unit = {
   // val scaler = new StandardScaler(withMean = true, withStd = true).fit(data.map(x => x.features))
  //  val dataS = data map { p => LabeledPoint(p.label, scaler.transform(p.features)) }
    val dataS = data

    try
    {
      FileUtils.deleteDirectory(new File("sentimentTrain/svm0vs1"))
      FileUtils.deleteDirectory(new File("sentimentTrain/svm2vs1"))
      FileUtils.deleteDirectory(new File("sentimentTrain/svm0vs2"))
    }
    catch
      {
        case _ =>
      }

    var points = dataS filter(p => p.label != 2)

    val svm0vs1 = SVMWithSGD.train(points, sgdSteps, a, c)
    svm0vs1.save(sc, "sentimentTrain/svm0vs1")


    points = dataS filter(p => p.label != 0)
    points = points map (p =>
      if (p.label == 2) {
        LabeledPoint(0, p.features)
      }
      else {
        p
      })

    val svm2vs1 = SVMWithSGD.train(points, sgdSteps, a, c)
    svm2vs1.save(sc, "sentimentTrain/svm2vs1")

    points = dataS filter(p => p.label != 1)
    points = points map { p =>
      if (p.label == 2) {
        LabeledPoint(1, p.features)
      }
      else {
        p
      }
    }
    val svm0vs2 = SVMWithSGD.train(points, sgdSteps, a, c)
    svm0vs2.save(sc, "sentimentTrain/svm0vs2")
  }
}

class OneVsOneSVM(sc: SparkContext) extends Serializable {

  val svm0vs1 = SVMModel.load(sc, "sentimentTrain/svm0vs1")
  val svm0vs2 = SVMModel.load(sc, "sentimentTrain/svm0vs2")
  val svm2vs1 = SVMModel.load(sc, "sentimentTrain/svm2vs1")

  svm0vs1.clearThreshold()
  svm0vs2.clearThreshold()
  svm2vs1.clearThreshold()

//  val means = Vectors.dense((sc.textFile("means.txt") map { _.toDouble }).collect)
//  val stds  = Vectors.dense((sc.textFile("stds.txt")  map { _.toDouble }).collect)
 // val scalerModel = new StandardScalerModel(stds, means)

  def predict(features: Array[Double]): Double = {
    predict(Vectors.dense(features))
  }

  def predict(features: Vector): Double = {

   // val scaledFeatures = scalerModel.transform(features)
    val scaledFeatures = features

    val prediction0vs1 = svm0vs1.predict(scaledFeatures)
    val prediction0vs2 = svm0vs2.predict(scaledFeatures)
    val prediction2vs1 = svm2vs1.predict(scaledFeatures)

    voting(prediction0vs1, prediction2vs1, prediction0vs2)
    // votingWithMargin(prediction0vs1, prediction2vs1, prediction0vs2)
  }

  def votingWithMargin(prediction0vs1: Double,
                       prediction2vs1: Double,
                       prediction0vs2: Double) = {

    val label0Score = -prediction0vs1 - prediction0vs2
    val label1Score = prediction0vs1 + prediction2vs1
    val label2Score = prediction0vs2 - prediction2vs1

    // println(label0Score + "\n" + label1Score + "\n" + label2Score + "\n")

    if (label0Score > label1Score) {
      if (label0Score > label2Score) 0 else 2
    }
    else {
      if (label2Score > label1Score) 2 else 1
    }
  }

  def voting(prediction0vs1: Double,
             prediction2vs1: Double,
             prediction0vs2: Double): Double = {


    var label0Count = 0
    var label1Count = 0
    var label2Count = 0

    if (prediction0vs1 >= 0)
      label1Count += 1
    else
      label0Count += 1

    if (prediction0vs2 >= 0)
      label2Count += 1
    else
      label0Count += 1


    if (prediction2vs1 >= 0)
      label1Count += 1
    else
      label2Count += 1

    //println("Voting: " + label0Count + " " + label1Count + " " + label2Count + " " + "\n" +
    //  prediction0vs1 + " " + prediction0vs2 + " " + prediction2vs1)

    if (label0Count == label1Count && label0Count == label2Count) {
      println("warn: equals")
      1
    }
    else {
      if (label0Count > label1Count) {
        if (label0Count > label2Count) 0
        else 2
      }
      else {
        if (label1Count > label2Count) 1
        else 2
      }
    }
  }
}



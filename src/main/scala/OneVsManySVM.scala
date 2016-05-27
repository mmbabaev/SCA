import java.io.File

import HelperFunctions.SparkContextSingleton
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.feature.{StandardScaler, StandardScalerModel}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.Saveable
import org.apache.spark.rdd.RDD
import org.apache.commons.io.FileUtils


/**
  * Created by Mihail on 04.04.16.
  */
object OneVsManySVM {
  def load(sc: SparkContext, data: RDD[LabeledPoint]): Unit = {

    try
    {
      FileUtils.deleteDirectory(new File("sentimentTrain/svm0vsAll"))
      FileUtils.deleteDirectory(new File("sentimentTrain/svm1vsAll"))
      FileUtils.deleteDirectory(new File("sentimentTrain/svm2vsAll"))
    }
    catch
    {
      case _ =>
    }


//    val scaler = new StandardScaler(withMean = true, withStd = true).fit(data.map(x => x.features))
//    val points = data map { p => LabeledPoint(p.label, scaler.transform(p.features)) }
    val points = data

    val points0vsAll = points map { point =>
      val label = point.label match {
        case 0 => 1
        case 1 => 0
        case 2 => 0
      }
      LabeledPoint(label, point.features)
    }

    val sgdSteps = 10
    val a = 1
    val c = 0.1

    val svm0 = SVMWithSGD.train(points0vsAll, sgdSteps, a, c)
    svm0.save(sc, "sentimentTrain/svm0vsAll")

    val points1vsAll = points map { point =>
      val label = point.label match {
        case 0 => 0
        case 1 => 1
        case 2 => 0
      }
      LabeledPoint(label, point.features)
    }

    val svm1 = SVMWithSGD.train(points1vsAll, sgdSteps, a, c)
    svm1.save(sc, "sentimentTrain/svm1vsAll")

    val points2vsAll = points map { point =>
      val label = point.label match {
        case 0 => 0
        case 1 => 0
        case 2 => 1
      }
      LabeledPoint(label, point.features)
    }

    val svm2 = SVMWithSGD.train(points2vsAll, sgdSteps, a, c)
    svm2.save(sc, "sentimentTrain/svm2vsAll")
  }
}

class OneVsManySVM(sc: SparkContext) extends Serializable {

  val svm0 = SVMModel.load(sc, "sentimentTrain/svm0vsAll")
  val svm1 = SVMModel.load(sc, "sentimentTrain/svm1vsAll")
  val svm2 = SVMModel.load(sc, "sentimentTrain/svm2vsAll")

  svm0.clearThreshold()
  svm1.clearThreshold()
  svm2.clearThreshold()

  // val means = Vectors.dense((sc.textFile("means.txt") map { _.toDouble }).collect)
  // val stds  = Vectors.dense((sc.textFile("stds.txt")  map { _.toDouble }).collect)
  // val scalerModel = new StandardScalerModel(stds, means)


  def predict(features: Vector): Double = {

    //  val scaledFeatures = scalerModel.transform(features)

    val label0Score = svm0.predict(features)
    val label1Score = svm1.predict(features)
    val label2Score = svm2.predict(features)

//    if (label2Score >= 0) 2
//    else {
//      if (label0Score >= 0) 0 else 1
//    }
    //println((label0Score > 0) + " " + (label1Score > 0) + " " + (label2Score > 0))
        if (label1Score >= 0)
          1
        else {
          val label0 = label0Score >= 0
          val label2 = label2Score >= 0

          if (label0) return 0
          if (label2) return 2

          if (label2Score >= label0Score) 2 else 0

    //      if (label0 && label2 || !(label0 || label2))
    //        1
    //      else {
    //        if (label0) 0 else 2
    //      }
        }
      }

//    if (label1Score >= label0Score) {
//      if (label1Score > label2Score) 1 else 2
//    }
//    else {
//      if (label2Score < label0Score) 0 else 2
//    }

//    if (label0Score > label1Score) {
//      if (label0Score > label2Score) 0 else 2
//    }
//    else {
//      if (label2Score > label1Score) 2 else 1
//    }

  def predict(features: Array[Double]): Double = {
    predict(Vectors.dense(features))
  }

  def predictions(features: Vector) = {
    Array(svm0.predict(features), svm1.predict(features), svm2.predict(features))
  }
}

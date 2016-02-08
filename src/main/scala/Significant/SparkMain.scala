package Significant

import org.apache.spark.mllib.classification.{SVMWithSGD, SVMModel, NaiveBayes}
import org.apache.spark.mllib.evaluation.{BinaryClassificationMetrics, MulticlassMetrics}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Mihail on 08.02.16.
  */
object SparkMain extends App {
  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[1]")
  val sc = new SparkContext(conf)

  val lines = sc.textFile("significantFeatures.txt")

  val points = lines map { line =>
    val ar = line.split("\t")
    val features = ar(2).split(" ") map (i => i.toDouble)
    LabeledPoint(ar(3).toDouble, Vectors.dense(features))
  }

  val data = points

    val splits = data.randomSplit(Array(0.7, 0.3))

    val training = data
    val test = splits(1)
    println("Count: " + data.count)
    // val model = SVMWithSGD.train(training, 1)
    val model = NaiveBayes.train(training)

    // Compute raw scores on the test set.
    val scoreAndLabels = test.map { point =>

      val score = model.predict(point.features)
      println(point.features + "\t" + point.label + score)
      (score, point.label)
    }



    val metrics = new MulticlassMetrics(scoreAndLabels)

    println("Metrics: F(0) = " + metrics.fMeasure(0) + "\nF(1) = " +
      metrics.fMeasure(1) + "\n" +
      "F = " + metrics.fMeasure)

//  val recall = metrics.recall(1)
//  val precision = metrics.precision(1)

//  val f = 2.0 * precision * recall / (precision + recall)

//  val tp = test.filter { point =>
//    val score = model.predict(point.features)
//    score == 1 && score == point.label
//  }
//
//  val fp = test.filter { point =>
//    val score = model.predict(point.features)
//    score == 1 && score != point.label
//  }
//
//  val fn = test.filter { point =>
//    val score = model.predict(point.features)
//    score == 0 && point.label == 1
//  }
//
//  val Precision = tp.count * 1.0 / (tp.count + fp.count)
//  val Recall = tp.count * 1.0 / (tp.count + fn.count)
//  val f = 2 * Precision * Recall / (Precision + Recall)

//  println("MY TP = " + tp.count + "\n" + fp.count + "\n" + fn.count + "\n" + metrics.truePositiveRate(1))
//  println("Recall = " + Recall + "\nPrecision = " + Precision + "\n" + "F = " + f + "\n")


//  // Get evaluation metrics.
//  val metrics = new BinaryClassificationMetrics(scoreAndLabels)
//  val auROC = metrics.areaUnderROC()
//
//  println("Area under ROC = " + auROC)
//  println("F measure = " + metrics.fMeasureByThreshold(1))
//  println("F0 measure = " + metrics.fMeasureByThreshold(0))


//
//  println("RESULT:")
//  val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
//
//    val prediction = model.predict(features)
//    println(prediction + " " + label)
//    (prediction, label)
//  }
//
//  val metrics = new MulticlassMetrics(predictionAndLabels)
//
//  println("F: " + metrics.fMeasure(1))
//  println("recall: " + metrics.recall(1))
//  println("prediction: " + metrics.precision(1))
}

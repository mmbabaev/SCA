package Significant

import GaussianNaiveBayes.GaussianNaiveBayes
import org.apache.spark.mllib.classification._
import org.apache.spark.mllib.evaluation.{BinaryClassificationMetrics, MulticlassMetrics}
import org.apache.spark.mllib.feature.StandardScaler
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.DecisionTree
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

import scala.reflect.io.File
import scalax.chart.module.ChartFactories.BarChart

/**
  * Created by Mihail on 08.02.16.
  */

object SparkMain extends App {
  import org.apache.log4j.Logger
  import org.apache.log4j.Level

  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[4]")
  val sc = new SparkContext(conf)

  val fileName = "significantFeatures.txt"
  val lines = sc.textFile(fileName)


  val points = lines map { line =>
    val ar = line.split("\t")
    val features = ar(2).split(" ") map (i => i.toDouble)
    LabeledPoint(ar(3).toDouble, Vectors.dense(features))
  }

 // createChartsForFeatures(points)

  var f0 = 0.0
  var p0 = 0.0
  var r0 = 0.0
  val count = 1000

  for (i <- 0 until count) {
    val metrics = getMetrics()
    f0 += metrics.fMeasure(0)
    p0 += metrics.precision(0)
    r0 += metrics.recall(0)
  }
  sc.stop()

  f0 = f0 / count
  p0 = p0 / count
  r0 = r0 / count

  println("TOTAL:")
  println("Summary Statistics")
  println(s"Precision 0  = $p0")
  println(s"Recall 0 = $r0")
  println(s"F 0 score = $f0")

  def getMetrics() = {
//    val scaler = new StandardScaler(withMean = true, withStd = true).fit(points.map(x => x.features))
//    val data = points map { p => LabeledPoint(p.label, scaler.transform(p.features)) }

    val data = points

    // Run training algorithm to build the model
//    val numClasses = 2
//    val categoricalFeaturesInfo = Map[Int, Int]()
//    val impurity = "gini"
//    val maxDepth = 10
//    val maxBins = 20

    val Array(training, test) = data.randomSplit(Array(0.65, 0.35))


//    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")
//    val model = DecisionTree.trainClassifier(training, numClasses, categoricalFeaturesInfo, impurity, maxDepth, maxBins)
//    val model = new LogisticRegressionWithLBFGS().setNumClasses(2).run(training)

 //   val model = SVMWithSGD.train(training, 100)

    val model = new GaussianNaiveBayes(training, Seq(0.0, 1.0).toArray)

    // Compute raw scores on the test set
    val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
      val prediction = model.predict(features)
      (prediction, label)
    }

    // Instantiate metrics object
    val metrics = new MulticlassMetrics(predictionAndLabels)

    // Overall Statistics
    val f0 = metrics.fMeasure(0)
    val p0 = metrics.precision(0)
    val r0 = metrics.recall(0)


    println("Summary Statistics")
    println(s"Precision 0  = $p0")
    println(s"Recall 0 = $r0")
    println(s"F 0 score = $f0")

    metrics
  }

  def createChartsForFeatures(points: RDD[LabeledPoint]): Unit = {
    val titles = ("f1 (formal citations) , f2 (authors) , f3 (acronyms) , " +
      "f5 (starts with pronoun) , f7 (citation list) , f8 (work nouns)").split(", ")

    for(featureNumb <- 0 until titles.length) {
      val data = (0 to 10) map {i => (i, points.filter(p => p.features(featureNumb) == i && p.label == 0).count())}
      val chart = BarChart(data, titles(featureNumb))
      chart.show()
    }
  }
  //    val splits = data.randomSplit(Array(0.6, 0.4))
//
//    val training = splits(0)
//    val test = splits(1)
//    println("Count: " + data.count)
//
//    val model = SVMWithSGD.train(training, 1)
//    //val model = NaiveBayes.train(training, 1.0)
//
//    // Compute raw scores on the test set.
//    val scoreAndLabels = test.map { point =>
//
//      val score = model.predict(point.features)
//      println(point.features + "\t" + point.label + " " + score)
//      (score, point.label)
//    }
//
//
//
//    val metrics = new BinaryClassificationMetrics(scoreAndLabels)
//
//    for (a <- metrics.fMeasureByThreshold()) {
//      println("A: = " + a)
//    }

//    println("Metrics: F(0) = " + metrics.fMeasure(0) + "\nF(1) = " +
//      metrics.fMeasure(1) + "\n" +
//      "F = " + metrics.fMeasure)

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

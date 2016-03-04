package GaussianNaiveBayes

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.mllib.classification.ClassificationModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ListBuffer

class GaussianNaiveBayes(dataset: RDD[LabeledPoint], labels: RDD[Double]) extends ClassificationModel {
  var pointsOfClass = Map[Double, RDD[LabeledPoint]]()
    labels.foreach { label =>
    val points = dataset filter { p => p.label == label }
    pointsOfClass += (label -> points)
  }

  var summaries = summarizedByClass(dataset)

  def predict(testData: RDD[Vector]): RDD[Double] = {
    throw new Exception("not implemented yet!")
  }


  def predict(testData: Vector): Double = {
    val probs = calculateClassProbabilities(summaries, testData.toArray)
    val max = probs.values.max
    probs.keys.filter(k => probs(k) == max).toList.head
  }

  def calculateClassProbabilities(summaries: Map[Double, RDD[(Double, Double)]],
                                  inputVector: Array[Double]) = {
    var probabilities = Map[Double, Double]()

    for ((label, rddLabelSummaries) <- summaries) {
      val labelSummaries = rddLabelSummaries.collect()
      probabilities += (label -> 1)
      for (i <- 0 to labelSummaries.length) {
        val mean = labelSummaries(i)._1
        val std = labelSummaries(i)._2
        val x = inputVector(i)
        val prob = probabilities(label) * calculatePabability(x, mean, std)
        probabilities -= label
        probabilities += (label -> prob)
      }
    }
    probabilities
  }

  def calculatePabability(x: Double, mean: Double, std: Double) = {
    val exponent = math.exp(-(math.pow(x - mean, 2) /
      (2 * math.pow(std, 2))))
    (1 / (math.sqrt(2 * math.Pi) * std)) * exponent
  }

  def summarizedByClass(dataset: RDD[LabeledPoint]) = {
    var summaries = Map[Double, RDD[(Double, Double)]]()

    for ((label, points) <- pointsOfClass) {
      summaries += (label -> summarize(points))
    }
    summaries
  }

  def summarize(dataset: RDD[LabeledPoint]): RDD[(Double, Double)] = {
    val result = dataset map {
      point => {
        val features = point.features.toArray
        ( meanNumbers(features), stdev(features) )
      }
    }
    result
  }

  def meanNumbers(numbers: Array[Double]) = {
    sum(numbers) * 1.0 / numbers.length
  }

  def stdev(numbers: Array[Double]) = {
    val avg = meanNumbers(numbers)
    val variance = sum(numbers.map(n => math.pow(n - avg, 2))) /
      (numbers.length -1).toDouble
    math.sqrt(variance)
  }

  def sum(numbers: Array[Double]) = {
    var sum = 0.0
    for (numb <- numbers) {
      sum += numb
    }
    sum
  }
}




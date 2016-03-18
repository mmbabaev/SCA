package GaussianNaiveBayes

import org.apache.spark.api.java.JavaRDD
import org.apache.spark.mllib.classification.ClassificationModel
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ListBuffer

class GaussianNaiveBayes(dataset: RDD[LabeledPoint], labels: Array[Double]) extends ClassificationModel {
  var pointsOfClass = Map[Double, RDD[LabeledPoint]]()
    labels.foreach { label =>
    val points = dataset filter { p => p.label == label }
    pointsOfClass += (label -> points)
  }

  var summaries = summarizedByClass()

  def predict(testData: RDD[Vector]): RDD[Double] = {
    throw new Exception("not implemented yet!")
  }

  def predict(testData: Vector): Double = {
    val probs = calculateClassProbabilities(summaries, testData.toArray)
    var max = 0.0
    var result = 0.0

    for ((k, v) <- probs) {
      if (v > max) {
        result = k
        max = v
      }
    }

    result
    //probs.keys.filter(k => probs(k) == max).toList.head
  }

  def calculateClassProbabilities(summaries: Map[Double, Array[(Double, Double)]],
                                  inputVector: Array[Double]) = {
    var probabilities = Map[Double, Double]()

    for ((label, labelSummaries) <- summaries) {
      probabilities += (label -> 1)
      for (i <- labelSummaries.indices) {
        val mean = labelSummaries(i)._1
        val std = labelSummaries(i)._2
        val x = inputVector(i)
        val prob = probabilities(label) * calculateProbability(x, mean, std)
        probabilities -= label
        probabilities += (label -> prob)
      }
    }
    probabilities
  }

  def calculateProbability(x: Double, mean: Double, std: Double) = {
    val exponent = math.exp(-(math.pow(x - mean, 2) /
      (2 * math.pow(std, 2))))
    (1 / (math.sqrt(2 * math.Pi) * std)) * exponent
  }

  def summarizedByClass() = {
    var summaries = Map[Double, Array[(Double, Double)]]()

    for ((label, points) <- pointsOfClass) {
      summaries += (label -> summarize(points))
    }

    summaries
  }

  def summarize(dataset: RDD[LabeledPoint]): Array[(Double, Double)] = {
    val featuresList = (dataset map { p => p.features.toArray }).collect()
    val result = featuresList.head.indices map {
      index =>
        val numbers = featuresList map {
          features => features(index)
        }
        (meanNumbers(numbers), stdev(numbers))
    }

    result.toArray
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




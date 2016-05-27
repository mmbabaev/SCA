package GaussianNaiveBayes

import java.io.PrintWriter

import org.apache.spark.mllib.classification.ClassificationModel
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import scala.io.Source

class GaussianNaiveBayes() extends ClassificationModel {

  var summaries = Map[Double, Array[(Double, Double)]]()

  private var pointsOfClass = Map[Double, RDD[LabeledPoint]]()

  def this(dataset: RDD[LabeledPoint]) = {
    this()
    val labels = (dataset map { _.label }).distinct().collect()


    labels.foreach { label =>
      val points = dataset filter { p => p.label == label }
      pointsOfClass += (label -> points)
    }

    summaries = summarizedByClass()
  }

  def predict(testData: RDD[Vector]): RDD[Double] = {
    throw new Exception("not implemented yet!")
  }

  def predict(testData: Array[Double]): Double = {
    predict(Vectors.dense(testData))
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

  def save(filePath: String): Unit = {
    val pw = new PrintWriter(filePath)
    for ((label, ar) <- summaries) {
      pw.write(label + "\t")
      for (elem <- ar) {
        pw.write(elem._1 + "_" + elem._2 + " ")
      }
      pw.write("\n")
    }
    pw.close()
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

object GaussianNaiveBayes {
  def load(filename: String) = {
    var summaries = Map[Double, Array[(Double, Double)]]()

    for (line <- Source.fromFile(filename).getLines()) {
      val tabSplit = line.split("\t")
      val label = tabSplit(0).toDouble
      val ar = tabSplit(1).split(" ") map { elem =>
        val spaceSplit = elem.split("_")
        (spaceSplit(0).toDouble, spaceSplit(1).toDouble)
      }

      summaries += (label -> ar)
    }

    val model = new GaussianNaiveBayes()
    model.summaries = summaries
    model
  }
}
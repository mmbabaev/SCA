
import org.apache.spark.mllib.evaluation.{MulticlassMetrics, BinaryClassificationMetrics}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.classification.{SVMWithSGD, NaiveBayesModel, NaiveBayes}

object Main extends App {
  val classificator = new SentimentClassificator("sentiment_corpus.txt", 1000)
  
  val points = classificator.citations map {c =>
    LabeledPoint(c.sentiment + 1, Vectors.dense(c.features))
  }

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[4]")
  val sc = new SparkContext(conf)

  val data = sc.parallelize(points)

  val splits = data.randomSplit(Array(0.6, 0.4), seed = 11L)
  val training = splits(0).cache()
  val test = splits(1)


  val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

  val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    val prediction = model.predict(features)
    (prediction, label)
  }

  val metrics = new MulticlassMetrics(predictionAndLabels)
  println("F: " + metrics.fMeasure)
}
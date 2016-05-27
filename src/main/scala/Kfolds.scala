import HelperFunctions.SparkContextSingleton
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.feature.{IDF, HashingTF}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils.kFold
import org.apache.spark.mllib.classification.NaiveBayes

object Kfolds extends App {
  val fileName = "sentiment_corpus.txt"
  val citations = Citation.getCitationsFromFile(fileName)
  println("COUNT: " + citations.length)
  val sc = SparkContextSingleton.getInstance

  val data = sc.parallelize(citations)
  val folds = kFold(data, 10, 10)

  val fMetrics = folds map { case (train, test) => 
    val trainDocs = TestModel.citationsToDocs(train)
    val testDocs = TestModel.citationsToDocs(test)

    val trainLabels = train map {
      _.sentiment
    }
    val testLabels = test map {
      _.sentiment
    }

    val hashingTF = new HashingTF()
    val tf = hashingTF.transform(trainDocs)
    //tf.saveAsObjectFile("sentimentTrain/tf")

    val idf = new IDF().fit(tf)
    val tfidf = idf.transform(tf)

    val zipped = trainLabels.zip(tfidf)

    /*Now we transform them into LabeledPoints*/
    val labeledPoints = zipped.map { case (label, vector) => LabeledPoint(label, vector) }
    labeledPoints.cache()

    OneVsManySVM.load(sc, labeledPoints)
    val model = new OneVsManySVM(sc)

//      OneVsOneSVM.load(sc, labeledPoints)
//      val model = new OneVsOneSVM(sc)

//	 val model = NaiveBayes.train(labeledPoints, lambda = 1)

    val testVectors = testDocs map { doc =>
      val testTfidf = hashingTF.transform(doc)
      idf.transform(testTfidf)
    }
    val zippedTest = testLabels.zip(testVectors)
    val testPoints = zippedTest map { case (label, vector) => LabeledPoint(label, vector) }


    val predictionAndLabels = testPoints.map { case LabeledPoint(label, features) =>
      val prediction = model.predict(features)
      (prediction, label)
    }

    val metrics = new MulticlassMetrics(predictionAndLabels)
    val macroF = (metrics.fMeasure(0) + metrics.fMeasure(1) + metrics.fMeasure(2)) / 3.0
    val microF = metrics.weightedFMeasure
    (microF, macroF)
  }

  println("Macro: " + fMetrics.map(_._2).reduce(_ + _) / fMetrics.length)
    println("Micro: " + fMetrics.map(_._1).reduce(_ + _) / fMetrics.length)
}
package Sentiment

import java.io.PrintWriter

import edu.arizona.sista.processors.{Sentence, Processor}
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.struct.{DirectedGraph, DirectedGraphEdgeIterator}
import org.apache.spark.{SparkContext, SparkConf}

import scala.annotation.tailrec
import scala.io.Source

/**
  * Created by Mihail on 18.03.16.
  */
class SentimentClassifier {
  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[1]")
  val sc = new SparkContext(conf)

  // todo: загружать заранее обученный классификатор
}


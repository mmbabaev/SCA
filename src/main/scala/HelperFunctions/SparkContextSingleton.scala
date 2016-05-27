package HelperFunctions

import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by Mihail on 01.04.16.
  */
object SparkContextSingleton {
  Logger.getLogger("org").setLevel(Level.ERROR)
  Logger.getLogger("akka").setLevel(Level.ERROR)

  val conf = new SparkConf().setAppName("SentimentAnalyse").setMaster("local[*]")
  val sc = new SparkContext(conf)

  def getInstance = sc
}

package HelperFunctions

import org.apache.spark.mllib.feature.{IDF, HashingTF}
import org.apache.spark.mllib.linalg.{Vector, SparseVector}
import org.apache.spark.rdd.RDD

case class IdfModel(documents: RDD[Seq[String]]) {
  val hashingTF = new HashingTF()
  val tf = hashingTF.transform(documents)

  val idfModel = new IDF().fit(tf)
  val idf = idfModel.idf
  val wordsCount = idf.size

  def transform(documents: RDD[Seq[String]]) = {
    documents map {
      doc =>
        transformDocument(doc)
    }
  }

  def transformDocument(document: Seq[String]) = {
    val indices = document map {
      word =>
        hashingTF.indexOf(word)
    }

    val values = indices map {
      i =>
        idf(i)
    }
    new SparseVector(wordsCount, indices.toArray, values.toArray)
  }
}

//case class IdfModel(documents: RDD[Seq[String]]) {
//  val N: Int = documents.count.asInstanceOf[Int]
//
//  val words = documents.flatMap(w => w).distinct
//  val wordsCount = words.count().asInstanceOf[Int]
//
//  val wordIndices: Map[String, Int] = words.zipWithIndex.map {
//    case (w, i) =>
//      (w, i.asInstanceOf[Int])
//  }.collect.toMap
//
//  val counts = collection.mutable.HashMap.empty[String, Int]
//  for (doc <- documents.collect()) {
//    for (w <- doc.distinct) {
//      counts.put(w, counts.getOrElse(w, 0) + 1)
//    }
//  }
//
//
//  val idf: Seq[Double] = (words map {
//    w =>
//      math.log(N / counts(w) )
//  }).collect
//
//  def transform(documents: RDD[Seq[String]]) = {
//    documents map {
//      doc =>
//        transformDocument(doc)
//    }
//  }
//
//  def transformDocument(document: Seq[String]) = {
//    val indices = document map { wordIndices.getOrElse(_, -1) } filter (_ != -1)
//    val values = indices map { i => idf(i) }
//    new SparseVector(wordsCount, indices.toArray, values.toArray)
//  }
//}


//class IdfModel(documents: Seq[Seq[String]]) {
//  val N = documents.length
//
//  val words = documents.flatMap(w => w).distinct
//
//  val wordIndices = words.zipWithIndex.toMap
//  val counts = (words map {
//    case w =>
//      val counts = documents.count(_.contains(w))
//      w -> counts
//  }).toMap
//
//  val idf = words map {
//    w =>
//      math.log(N / counts(w) )
//  }
//
//  def transform(documents: Seq[Seq[String]]) = {
//    documents map {
//      doc =>
//        transformDocument(doc)
//    }
//  }
//
//  def transformDocument(document: Seq[String]) = {
//    val indices = document map { wordIndices(_) }
//    val values = indices map { idf(_) }
//    new SparseVector(words.length, indices.toArray, values.toArray)
//  }
//}

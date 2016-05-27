package HelperFunctions.Extensions

import edu.arizona.sista.processors.fastnlp.FastNLPProcessor
import edu.arizona.sista.processors.Sentence
import edu.arizona.sista.struct.DirectedGraphEdgeIterator

import scala.collection.mutable.ArrayBuffer

object NlpExtension {

  implicit def sentenceDependencies(sentence: Sentence) = new {
    def dependencyList = {
      val depList = new ArrayBuffer[String]()

      sentence.dependencies.foreach(dependencies => {
        val iterator = new DirectedGraphEdgeIterator[String](dependencies)
        while(iterator.hasNext) {
          val dep = iterator.next

//          if (!dep._3.contains("punct") && !dep._3.contains("det") && ! dep._3.equals("num")) {
//            val words = sentence.words
//            depList.append(dep._3 + "_" + words(dep._1) + "_" + words(dep._2))
//          }
          if (!dep._3.contains("punct") && ! dep._3.equals("num")) {
            val words = sentence.words
            depList.append(dep._3 + "_" + words(dep._1) + "_" + words(dep._2))
          }
        }
      })

      depList.toList
    }
  }



}

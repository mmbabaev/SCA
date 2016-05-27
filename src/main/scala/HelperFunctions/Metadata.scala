package HelperFunctions

import java.io.PrintWriter
import scala.io.Source
import HelperFunctions.Extensions.StringExtension._


object Metadata {
  def authorAndYear(id: String) = {
    metadata(id)
  }

  /*

  i -> id = {D10-1001}
  i + 1 -> author = {Rush, Alexander M.; Sontag, David; Collins, Michael John; Jaakkola, Tommi}
  i + 2 -> title = {On Dual Decomposition and Linear Programming Relaxations for Natural Language Processing}
  i + 3 -> venue = {EMNLP}
  i + 4 -> year = {2010}

   */

  // todo: сериализация?
  val metadata = {
    val data = Source.fromURL(getClass.getResource("/acl-metadata.txt")).getLines().toList
    var i = 0
    var map = Map[String, (String, String)]()

    while (i < data.length) {

      val id = data(i).split("\\{")(1).dropRight(1)
      val author = data(i + 1).split(",").head.split("\\{")(1)
      val year = data(i + 4).split("\\{")(1).dropRight(1)
      map += (id -> (author, year))
      i += 6
    }

    println("metadata loaded\n")
    map
  }
}

object MetadataTest extends App {
  println(Metadata.authorAndYear("D10-1100"))
  println(Metadata.authorAndYear("D10-1099"))
}

object MetadataFix extends App {
  val text = "Then the words are tagged as inside a phrase (I), outside a phrase (O) or beginning of a phrase (B) (Ramhsaw and Marcus, 1995)."
  println(text.replaceHarvardCitationsWithToken("Ramshaw", "1995"))


//  val data = Source.fromFile("acl-metadata.txt").getLines().mkString("\n")
//  val newData = data.replaceAll("\n\\}", "\\}")
//  val pw = new PrintWriter("fix-acl-metadata.txt")
//  pw.write(newData)
//  pw.close()
}


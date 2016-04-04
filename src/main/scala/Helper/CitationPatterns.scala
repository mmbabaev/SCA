package Helper



object CitationPatterns {
//  val author = "[A-Z]([A-Z]|[a-z]| et al.? ?)* ?"
//  val authors = s"$author( ?,( and )? ?$author)*"
//  val authorHarvard1 = "[A-Z]([A-Z]|[a-z]| et al.? ?)* ?(,|;)? ?[0-9]{4}"
  // Автор в скобках
  //val HARVARD = s"\\(($authorHarvard1)( ?;? ?$authorHarvard1)*\\)".r
  val HARVARD = "\\( ?[^\\(]*[A-Z][^\\(]*[0-9]{4}[a-z]? ?\\)"
  // автор вне скобок
  val HARVARD2 = "[A-Z]([A-Z]|[a-z]| et al.? ?)*((, ?| ?and ?)?[A-Z]([a-z]|M)*)* ?\\( ?[0-9]{4}[a-z]? ?( ?(;|,)? ?[0-9]{4}[a-z]? ?)*\\)".r
  // автор, дата
  val HARVARD3 = "[A-Z]([A-Z]|[a-z]| et al.? ?)*((, ?| ?and ?)?[A-Z]([a-z]|M)*)* ?,? ?[0-9]{4}[a-z]? ?( ?;? ?[0-9]{4}[a-z]?)*".r

  val VANCOUVER = "\\[([0-9]| |,|-)+\\]".r
  val ANY = ("((" + HARVARD + ")|(" + HARVARD2 + ")|(" + HARVARD3 + ")|(" + VANCOUVER + "))").r
}



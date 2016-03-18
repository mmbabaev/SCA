

object CitationPatterns {
  val HARVARD = "\\(([\\w\\&\\.\\s]+,\\s\\d{4}(;\\s+[\\w\\&\\.\\s]+,\\s\\d{4})*)\\)"
  val HARVARD2 = "([A-Z][a-z]*)? ((and|&) )?[A-Z][a-z]* \\([0-9]{4}\\)"
  val VANCOUVER = "\\[[0-9]*-?[0-9]*\\]"
  val ANY = "((" + HARVARD + ")|(" + HARVARD2 + ")|(" + VANCOUVER + "))".r
}
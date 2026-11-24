package au.id.jazzy.erqx.engine.site

object Summary {

  val DefaultWords = 40

  private val ScriptsAndStyles = """(?is)<(script|style)\b.*?</\1\s*>""".r
  private val Tag = "<[^>]*>".r
  // Words, including internal apostrophes so "don't" isn't split in two. Unicode aware.
  private val Word = """(?U)\s*(\w+(?:['’]\w+)*)""".r

  /**
   * Summarise some rendered HTML as plain text, consisting of the first `maxWords` words, separated by single spaces,
   * and followed by an ellipsis if there was more.
   */
  def of(html: String, maxWords: Int = DefaultWords): String = {
    val text = unescape(Tag.replaceAllIn(ScriptsAndStyles.replaceAllIn(html, " "), " "))
      .replace("<", "").replace(">", "")
    val words = Word.findAllMatchIn(text).map(_.group(1))
    val taken = words.take(maxWords + 1).toList
    if (taken.size > maxWords) taken.take(maxWords).mkString(" ") + "…" else taken.mkString(" ")
  }

  private val Entity = """&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);""".r
  private val named = Map("amp" -> "&", "lt" -> "<", "gt" -> ">", "quot" -> "\"", "apos" -> "'", "nbsp" -> " ",
    "rsquo" -> "’", "lsquo" -> "‘", "ndash" -> "–", "mdash" -> "—", "hellip" -> "…")

  private def unescape(s: String): String = Entity.replaceAllIn(s, m => {
    val e = m.group(1)
    val replacement =
      if (e.startsWith("#x") || e.startsWith("#X")) codePoint(e.drop(2), 16)
      else if (e.startsWith("#")) codePoint(e.drop(1), 10)
      else named.get(e)
    scala.util.matching.Regex.quoteReplacement(replacement.getOrElse(m.matched))
  })

  private def codePoint(digits: String, radix: Int): Option[String] =
    scala.util.Try(new String(Character.toChars(Integer.parseInt(digits, radix)))).toOption
}

package au.id.jazzy.erqx.engine.site

import java.time.{ZoneId, ZonedDateTime}

import au.id.jazzy.erqx.engine.models._

object MetaDataParser {

  private val NameFormatExtractor = """(.+)\.([^\.]+)""".r
  private val NameAttributeExtractor = """(\d{4})-(\d{1,2})-(\d{1,2})-(.+)""".r

  /**
   * Split a file into its YAML front matter, and its body. If there is no front matter, the YAML is empty and the body
   * is the whole file.
   */
  def splitFrontMatter(content: String, timezone: ZoneId): (Yaml, String) = {
    val lines = content.split("\r?\n", -1).toList
    val afterBlank = lines.dropWhile(_.trim.isEmpty)
    afterBlank match {
      case first :: rest if first.trim == "---" =>
        val (front, remaining) = rest.span(_.trim != "---")
        (Yaml.parse(front.map(_.trim).mkString("\n"), timezone), remaining.drop(1).mkString("\n"))
      case _ => (Yaml.empty, content)
    }
  }

  /**
   * Parse the metadata for a blog post from its front matter and file name. The returned post has no content or summary.
   *
   * @param yaml The front matter
   * @param path The path of the blog post
   * @param name The file name of the blog post
   */
  def parsePost(yaml: Yaml, path: String, name: String, timezone: ZoneId): BlogPost = {
    val props = Yaml(yaml.map -- Seq("title", "id", "date", "tags"))
    val fTitle = yaml.getString("title")
    val fId = yaml.getString("id")
    val fDate = yaml.getDate("date")
    val tags = yaml.getString("tags").toList.flatMap(_.split(" +").toList).filter(_.nonEmpty).map(_.replace('+', ' '))

    // Extract id and format from the name
    val (nId, format) = name match {
      case NameFormatExtractor(id, format) => (id, format)
      case _ =>
        Log.warn(s"Could not extract blog post format from name: $name")
        (name, "html")
    }

    // Extract attributes from the name
    val (nTitle, nDate, permalinkTitle) = nId match {
      case NameAttributeExtractor(year, month, day, title) =>
        (
          Some(title.replace('_', ' ')),
          Some(ZonedDateTime.of(year.toInt, month.toInt, day.toInt, 0, 0, 0, 0, timezone)),
          title
        )
      case _ =>
        Log.warn(s"Blog post file name not extractable: $name")
        (None, None, nId)
    }

    val id = fId getOrElse nId
    val title = fTitle orElse nTitle getOrElse id
    val date = fDate orElse nDate getOrElse ZonedDateTime.now(timezone)

    BlogPost(id, path, title, date, permalinkTitle, format, tags.distinct, props)
  }

  /**
   * Parse the metadata for a page from its front matter and file name. The returned page has no content.
   */
  def parsePage(yaml: Yaml, path: String, name: String): Page = {
    val props = Yaml(yaml.map -- Seq("title", "permalink"))
    val title = yaml.getString("title")
    val fPermalink = yaml.getString("permalink")

    val (nPermalink, format) = name match {
      case NameFormatExtractor(p, f) => (p, f)
      case _ =>
        Log.warn(s"Could not extract page format from name: $name")
        (name, "html")
    }

    val permalink = fPermalink.getOrElse(nPermalink + ".html").dropWhile(_ == '/')

    Page(permalink, format, path, title, props)
  }
}

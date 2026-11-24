package au.id.jazzy.erqx.engine.site

import au.id.jazzy.erqx.engine.models.BlogPost

/**
 * Generates URL paths for everything in the site.
 *
 * @param prefix The path the site is served from, eg "" if served from the root of a domain, or "/blog".
 *               Normalised so that it never ends in a slash.
 * @param baseUrl The absolute URL of the host, eg "https://jazzy.id.au", for URLs that must be absolute.
 */
class Router(prefix: String, baseUrl: String) {

  val path: String = {
    val trimmed = prefix.trim.stripPrefix("/").stripSuffix("/")
    if (trimmed.isEmpty) "" else "/" + trimmed
  }
  private val host = baseUrl.stripSuffix("/")

  def index: String = s"$path/"
  def view(post: BlogPost): String = path + post.toPermalink
  def year(year: Int): String = f"$path%s/$year%d.html"
  def month(year: Int, month: Int): String = f"$path%s/$year%d/$month%02d.html"
  def day(year: Int, month: Int, day: Int): String = f"$path%s/$year%d/$month%02d/$day%02d.html"
  def tag(tag: String): String = s"$path/tags/${UriEncoding.encodePathSegment(tag)}.html"
  def tags: String = s"$path/tags.html"
  def atom: String = s"$path/atom.xml"
  def notFound: String = s"$path/404.html"
  def themeAsset(file: String): String = s"$path/_assets/$file"
  def page(permalink: String): String = s"$path/${permalink.split('/').map(UriEncoding.encodePathSegment).mkString("/")}"

  def absolute(urlPath: String): String = host + urlPath

  /** The path of the file to write the given URL path to, relative to the root of the generated site. */
  def fileFor(urlPath: String): String = {
    val local = UriEncoding.decodePath(urlPath.stripPrefix(path)).stripPrefix("/")
    if (local.isEmpty || local.endsWith("/")) local + "index.html" else local
  }
}

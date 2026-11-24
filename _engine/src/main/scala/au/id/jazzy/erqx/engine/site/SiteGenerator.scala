package au.id.jazzy.erqx.engine.site

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, StandardCopyOption}
import java.time.format.DateTimeFormatter
import java.util.Locale

import au.id.jazzy.erqx.engine.models._
import au.id.jazzy.erqx.themes.jazzy.{html => theme}
import play.twirl.api.Html

import scala.jdk.CollectionConverters._

/**
 * Configuration for generating a site.
 *
 * @param baseDir The directory containing the blog content
 * @param outputDir The directory to generate the site into. It is deleted first.
 * @param assetsDir The directory containing the built theme assets
 * @param pathPrefix The path the site will be served from, eg "" or "/blog"
 * @param baseUrl The absolute URL of the host that the site will be served from, eg "https://jazzy.id.au"
 */
case class SiteConfig(baseDir: File, outputDir: File, assetsDir: File, pathPrefix: String, baseUrl: String)

class SiteGenerator(config: SiteConfig) {

  private val router = new Router(config.pathPrefix, config.baseUrl)
  private val outputDir = config.outputDir.getCanonicalFile
  private val baseDir = config.baseDir.getCanonicalFile

  private val FeedSize = 5
  private val themeAssets = Seq("erqx-jazzy-theme.css", "erqx-jazzy-theme.js", "erqx-icon.ico")

  def generate(): Unit = {
    val blog = new BlogLoader(baseDir).load()
    Log.info(s"Loaded ${blog.posts.size} posts and ${blog.pages.size} pages")

    prepareOutput()

    copyStaticFiles()
    copyThemeAssets()

    blog.posts.foreach(post => write(router.view(post), theme.blogPost(blog, router, post)))
    blog.pages.foreach { page =>
      write(router.page(page.permalink), theme.main(blog, router, page.title, page.properties.getString("head").map(Html.apply))(Html(page.content)))
    }

    generateLists(blog)

    write(router.atom, Html(FeedFormatter.atom(blog, blog.posts.take(FeedSize), router)))
    write(router.notFound, theme.pageNotFound(blog, router))
    // Tell GitHub Pages to serve the files as is, rather than running Jekyll, which ignores underscore directories
    Files.write(new File(outputDir, ".nojekyll").toPath, Array.emptyByteArray)

    Log.info(s"Generated site in $outputDir")
  }

  // Lists

  private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

  private def generateLists(blog: Blog): Unit = {
    def title(t: String) = Some(t)
    def list(url: String, title: Option[String], groups: List[PostGroup], nav: ListNav): Unit =
      write(url, theme.postList(blog, router, title, groups, nav))

    def neighbours[A](all: List[A], current: A)(link: A => Link): (Option[Link], Option[Link]) = {
      // all is in reverse chronological order (or reverse alphabetical for tags), so the next item is the one before
      val i = all.indexOf(current)
      (all.lift(i + 1).map(link), if (i > 0) all.lift(i - 1).map(link) else None)
    }

    // Index, grouped by year
    list(router.index, None, blog.years.map(y => PostGroup(Some(yearLink(y.year)), y.posts)), ListNav.empty)

    val indexLink = Link("All posts", router.index)

    blog.years.foreach { year =>
      val (previous, next) = neighbours(blog.years, year)(y => yearLink(y.year))
      list(router.year(year.year), title(s"All posts for ${year.year}"),
        year.months.map(m => PostGroup(Some(monthLink(m)), m.posts)),
        ListNav(Some(indexLink), previous, next))
    }

    blog.months.foreach { month =>
      val (previous, next) = neighbours(blog.months, month)(monthLink)
      list(router.month(month.year, month.month), title(s"All posts for ${month.fullName}"),
        month.days.map(d => PostGroup(Some(Link(d.day.toString, router.day(d.year, d.month, d.day))), d.posts)),
        ListNav(Some(yearLink(month.year)), previous, next))
    }

    blog.days.foreach { day =>
      val (previous, next) = neighbours(blog.days, day)(dayLink)
      val monthName = Month.nameOf(day.month)
      list(router.day(day.year, day.month, day.day), title(s"All posts for ${day.day} $monthName ${day.year}"),
        List(PostGroup(None, day.posts)),
        ListNav(Some(Link(s"$monthName ${day.year}", router.month(day.year, day.month))), previous, next))
    }

    // Tags
    val tagNames = blog.tags.keys.toList
    blog.tags.foreach { case (tag, posts) =>
      // tagNames is alphabetical, the opposite order to the date lists
      val i = tagNames.indexOf(tag)
      val previous = if (i > 0) Some(tagLink(tagNames(i - 1))) else None
      val next = tagNames.lift(i + 1).map(tagLink)
      list(router.tag(tag), title(s"All posts for tag $tag"), List(PostGroup(None, posts)),
        ListNav(Some(Link("All tags", router.tags)), previous, next))
    }
    write(router.tags, theme.tagList(blog, router, blog.tags.toList.map { case (tag, posts) => tag -> posts.size }))
  }

  private def yearLink(year: Int) = Link(year.toString, router.year(year))
  private def monthLink(m: Month) = Link(m.fullName, router.month(m.year, m.month))
  private def dayLink(d: Day) = Link(s"${d.day} ${Month.nameOf(d.month)} ${d.year}", router.day(d.year, d.month, d.day))
  private def tagLink(tag: String) = Link(tag, router.tag(tag))

  // Files

  private def write(urlPath: String, content: Html): Unit = write(urlPath, content.body)

  private def write(urlPath: String, content: String): Unit = {
    val file = new File(outputDir, router.fileFor(urlPath))
    if (file.exists()) sys.error(s"Two things generated at ${file}")
    Files.createDirectories(file.getParentFile.toPath)
    Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
  }

  private def prepareOutput(): Unit = {
    if (outputDir == baseDir || baseDir.toPath.startsWith(outputDir.toPath) || outputDir.toPath.getNameCount == 0) {
      sys.error(s"Refusing to use $outputDir as the output directory, it is, or contains, the blog directory")
    }
    deleteRecursively(outputDir)
    Files.createDirectories(outputDir.toPath)
  }

  private def deleteRecursively(dir: File): Unit = if (dir.exists()) {
    val stream = Files.walk(dir.toPath)
    try stream.iterator().asScala.toList.reverse.foreach(p => Files.delete(p)) finally stream.close()
  }

  /** Copy everything that isn't hidden or underscore prefixed, as is. */
  private def copyStaticFiles(): Unit = {
    def copy(dir: File): Unit = dir.listFiles().sortBy(_.getName).foreach { f =>
      val name = f.getName
      if (!name.startsWith("_") && !name.startsWith(".") && f.getCanonicalFile != outputDir) {
        val target = new File(outputDir, baseDir.toPath.relativize(f.getCanonicalFile.toPath).toString)
        if (f.isDirectory) {
          copy(f)
        } else {
          Files.createDirectories(target.getParentFile.toPath)
          Files.copy(f.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING)
        }
      }
    }
    copy(baseDir)
  }

  private def copyThemeAssets(): Unit = themeAssets.foreach { name =>
    val source = new File(config.assetsDir, name)
    if (!source.isFile) {
      sys.error(s"Theme asset $source not found, run via sbt so that the assets are built first, or pass --assets")
    }
    val target = new File(outputDir, router.fileFor(router.themeAsset(name)))
    Files.createDirectories(target.getParentFile.toPath)
    Files.copy(source.toPath, target.toPath)
  }
}

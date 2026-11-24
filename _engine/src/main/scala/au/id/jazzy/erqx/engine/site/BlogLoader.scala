package au.id.jazzy.erqx.engine.site

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.ZoneId

import au.id.jazzy.erqx.engine.models._
import play.doc.{PlayDoc, PlayDocTemplates}

import scala.jdk.CollectionConverters._

/**
 * Loads a blog from the file system. Posts and pages are rendered, and posts summarised, exactly once, here.
 *
 * @param baseDir The directory containing `_config.yml`, `_posts`, `_pages` and `_code`.
 */
class BlogLoader(baseDir: File) {

  private val repo = new FileSystemRepository(baseDir)
  private val playDoc = new PlayDoc(repo, repo, "", "", None, PlayDocTemplates, None)

  def load(): Blog = {
    val info = loadInfo()
    new Blog(info, loadPosts(info.timezone), loadPages(info.timezone))
  }

  private def read(file: File): String = new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8)

  private def listFiles(dir: String): List[File] = {
    val d = new File(baseDir, dir)
    if (d.isDirectory) d.listFiles().filter(f => f.isFile && !f.getName.startsWith(".")).sortBy(_.getName).toList else Nil
  }

  private def render(body: String, format: String): String = format match {
    case "md" => playDoc.render(body, None)
    case _ => body
  }

  private def loadPosts(timezone: ZoneId): List[BlogPost] = listFiles("_posts").map { file =>
    val path = "_posts/" + file.getName
    try {
      val (yaml, body) = MetaDataParser.splitFrontMatter(read(file), timezone)
      val post = MetaDataParser.parsePost(yaml, path, file.getName, timezone)
      post.tags.find(t => t.contains('/') || t.contains('\\')).foreach { t =>
        sys.error(s"Tag '$t' contains a path separator")
      }
      val content = render(body, post.format)
      post.copy(content = content, summary = Summary.of(content))
    } catch {
      case e: Exception => throw new RuntimeException(s"Error loading post '$path'", e)
    }
  }

  private def loadPages(timezone: ZoneId): List[Page] = listFiles("_pages").map { file =>
    val path = "_pages/" + file.getName
    try {
      val (yaml, body) = MetaDataParser.splitFrontMatter(read(file), timezone)
      val page = MetaDataParser.parsePage(yaml, path, file.getName)
      page.copy(content = render(body, page.format))
    } catch {
      case e: Exception => throw new RuntimeException(s"Error loading page '$path'", e)
    }
  }

  private def loadInfo(): BlogInfo = {
    val configFile = new File(baseDir, "_config.yml")
    val config = if (configFile.isFile) Yaml.parse(read(configFile), ZoneId.systemDefault()) else Yaml.empty
    BlogInfo(
      title = config.getString("title").getOrElse("A blog with no name"),
      subTitle = config.getString("subTitle"),
      author = config.getString("author").getOrElse("No one"),
      language = config.getString("language").getOrElse("en"),
      description = config.getString("description"),
      footer = config.getString("footer"),
      timezone = config.getString("timezone").fold(ZoneId.systemDefault())(ZoneId.of),
      properties = config
    )
  }
}

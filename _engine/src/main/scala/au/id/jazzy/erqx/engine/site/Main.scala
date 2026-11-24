package au.id.jazzy.erqx.engine.site

import java.io.File

/**
 * Generates a static site.
 *
 * Usage: Main <base directory> [--output dir] [--assets dir] [--path-prefix /prefix] [--url https://example.com]
 */
object Main {

  private val usage =
    """Usage: <base directory> [options]
      |
      |  <base directory>     The directory containing the blog, with _config.yml, _posts, etc.
      |  --output <dir>       Where to generate the site. It is deleted first. Defaults to <base directory>/_site
      |  --assets <dir>       The built theme assets. Defaults to the web stage directory built by sbt
      |  --path-prefix <path> The path the site is served from, eg /blog. Defaults to none, ie the root.
      |  --url <url>          The absolute URL of the host serving the site, used for atom feeds.
      |                       Defaults to http://localhost:8000""".stripMargin

  def main(args: Array[String]): Unit = {
    val config = try parse(args.toList) catch {
      case e: IllegalArgumentException =>
        System.err.println(s"Error: ${e.getMessage}\n\n$usage")
        sys.exit(1)
    }
    new SiteGenerator(config).generate()
  }

  def parse(args: List[String]): SiteConfig = {
    def parseOptions(remaining: List[String], opts: Map[String, String]): (Map[String, String], List[String]) = remaining match {
      case ("--output" | "--assets" | "--path-prefix" | "--url") :: value :: rest =>
        parseOptions(rest, opts + (remaining.head -> value))
      case option :: _ if option.startsWith("--") => throw new IllegalArgumentException(s"Unknown or incomplete option $option")
      case positional :: rest =>
        val (o, p) = parseOptions(rest, opts)
        (o, positional :: p)
      case Nil => (opts, Nil)
    }

    val (opts, positional) = parseOptions(args, Map.empty)
    val baseDir = positional match {
      case dir :: Nil => new File(dir)
      case _ => throw new IllegalArgumentException("Exactly one base directory must be given")
    }
    if (!baseDir.isDirectory) throw new IllegalArgumentException(s"Base directory $baseDir does not exist")

    SiteConfig(
      baseDir = baseDir,
      outputDir = opts.get("--output").map(new File(_)).getOrElse(new File(baseDir, "_site")),
      assetsDir = opts.get("--assets").map(new File(_)).getOrElse(new File(baseDir, "_engine/target/web/stage")),
      pathPrefix = opts.getOrElse("--path-prefix", ""),
      baseUrl = opts.getOrElse("--url", "http://localhost:8000")
    )
  }
}

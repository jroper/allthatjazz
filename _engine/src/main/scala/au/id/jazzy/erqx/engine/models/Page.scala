package au.id.jazzy.erqx.engine.models

/**
 * A page
 *
 * @param permalink The permalink of the page, relative to the root of the blog, with no leading slash
 * @param format The format of the page
 * @param path The path where the page can be found
 * @param title The title of the page, if defined
 * @param content The rendered HTML of the page
 */
case class Page(permalink: String, format: String, path: String, title: Option[String], properties: Yaml = Yaml.empty,
                content: String = "")

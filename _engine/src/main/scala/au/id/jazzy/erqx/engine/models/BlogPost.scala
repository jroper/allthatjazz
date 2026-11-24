package au.id.jazzy.erqx.engine.models

import java.time.ZonedDateTime

import au.id.jazzy.erqx.engine.site.UriEncoding

/**
 * A blog post
 *
 * @param id The id of the blog post
 * @param path The path of the file that contains the blog post, relative to the blog's base directory
 * @param title The title of the blog post
 * @param date The date the post is published
 * @param permalinkTitle The title used in the permalink of the blog post
 * @param tags The tags associated with the blog post
 * @param content The rendered HTML of the post. Rendered once, when the blog is loaded.
 * @param summary The plain text summary of the post, used in lists of posts. Computed once, when the blog is loaded.
 */
final case class BlogPost(id: String, path: String, title: String, date: ZonedDateTime, permalinkTitle: String,
                          format: String, tags: List[String], properties: Yaml = Yaml.empty,
                          content: String = "", summary: String = "") {
  def toPermalink: String = "/%04d/%02d/%02d/%s.html".format(date.getYear, date.getMonthValue, date.getDayOfMonth,
    UriEncoding.encodePathSegment(permalinkTitle))
}

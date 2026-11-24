package au.id.jazzy.erqx.engine.site

import java.time.format.DateTimeFormatter

import au.id.jazzy.erqx.engine.models._

import scala.xml.{Elem, PCData}

object FeedFormatter {

  private val RelativeUrl = """(?i)(\b(?:href|src)\s*=\s*)(["'])(?![a-z][a-z0-9+.\-]*:|/|#|\?)(?:\./)?""".r

  /** Make relative links and images in some HTML absolute, since feed readers don't know about our base tag. */
  def absolutify(html: String, base: String): String =
    RelativeUrl.replaceAllIn(html, m =>
      scala.util.matching.Regex.quoteReplacement(m.group(1) + m.group(2) + base))

  def atom(blog: Blog, posts: List[BlogPost], router: Router): String = {

    val blogUpdate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(blog.lastUpdated)
    val indexUrl = router.absolute(router.index)

    val feed: Elem =
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/elements/1.1/">
        <title>{blog.info.title}</title>
        <link rel="alternate" type="text/html" href={indexUrl} />
        <link rel="self" type="application/atom+xml" href={router.absolute(router.atom)} />
        {blog.info.subTitle.map { subTitle =>
          <subtitle>{subTitle}</subtitle>
        }.toSeq}
        <id>{indexUrl}</id>
        <rights>{blog.info.author}</rights>
        <updated>{blogUpdate}</updated>
        <dc:creator>{blog.info.author}</dc:creator>
        <dc:date>{blogUpdate}</dc:date>
        <dc:language>{blog.info.language}</dc:language>
        <dc:rights>{blog.info.author}</dc:rights>
        {posts.map { post =>
          val postDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(post.date)
          // CDATA can't contain its own terminator
          val content = absolutify(post.content, indexUrl).replace("]]>", "]]]]><![CDATA[>")

          <entry>
            <title>{post.title}</title>
            <link rel="alternate" href={router.absolute(router.view(post))} />
            {post.tags.map { tag =>
              <category term={tag} scheme={router.absolute(router.tag(tag))} />
            }}
            <author>
              <name>{blog.info.author}</name>
              <uri>{indexUrl}</uri>
            </author>
            <id>{router.absolute(router.view(post))}</id>
            <updated>{postDate}</updated>
            <published>{postDate}</published>
            <content type="html">{PCData(content)}</content>
            <dc:date>{postDate}</dc:date>
          </entry>
        }}
      </feed>

    """<?xml version="1.0" encoding="UTF-8"?>""" + "\n" + feed.toString
  }
}

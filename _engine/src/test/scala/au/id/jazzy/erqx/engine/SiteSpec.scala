package au.id.jazzy.erqx.engine

import java.time.ZoneId

import au.id.jazzy.erqx.engine.models._
import au.id.jazzy.erqx.engine.site._
import org.scalatest.funsuite.AnyFunSuite

class SiteSpec extends AnyFunSuite {

  test("summary strips tags and keeps words") {
    assert(Summary.of("<p>Hello, <b>big</b> world!</p><img src='x.png'/>") == "Hello big world")
  }

  test("summary truncates with an ellipsis only when there is more") {
    val words = (1 to 60).map(i => s"w$i").mkString(" ")
    val summary = Summary.of(s"<p>$words</p>")
    assert(summary == (1 to 40).map(i => s"w$i").mkString(" ") + "…")
    assert(Summary.of(s"<p>one two</p>") == "one two")
  }

  test("summary handles entities, apostrophes and script content") {
    assert(Summary.of("<script>var x = 1;</script>Don&#39;t &amp; won&rsquo;t &lt;stop&gt;") == "Don't won’t stop")
  }

  test("front matter is split from the body") {
    val (yaml, body) = MetaDataParser.splitFrontMatter("\n---\ntitle: Foo\ntags: a b+c\n---\nBody\nmore", ZoneId.of("UTC"))
    assert(yaml.getString("title").contains("Foo"))
    assert(body == "Body\nmore")
    val (none, whole) = MetaDataParser.splitFrontMatter("No front matter", ZoneId.of("UTC"))
    assert(none == Yaml.empty && whole == "No front matter")
  }

  test("post metadata comes from the file name and front matter") {
    val (yaml, _) = MetaDataParser.splitFrontMatter("---\ntags: open+source scala\n---\n", ZoneId.of("UTC"))
    val post = MetaDataParser.parsePost(yaml, "_posts/2010-09-20-some_post.md", "2010-09-20-some_post.md", ZoneId.of("UTC"))
    assert(post.title == "some post" && post.permalinkTitle == "some_post" && post.format == "md")
    assert(post.tags == List("open source", "scala"))
    assert(post.toPermalink == "/2010/09/20/some_post.html")
  }

  test("router paths and files") {
    val router = new Router("/blog/", "https://example.com/")
    assert(router.index == "/blog/")
    assert(router.tag("open source") == "/blog/tags/open%20source.html")
    assert(router.fileFor(router.index) == "index.html")
    assert(router.fileFor(router.tag("open source")) == "tags/open source.html")
    assert(router.fileFor(router.month(2024, 3)) == "2024/03.html")
    assert(router.absolute(router.atom) == "https://example.com/blog/atom.xml")
    assert(new Router("", "x").index == "/")
  }

  test("blog groups posts by date and tag") {
    def post(date: String, name: String, tags: String) =
      MetaDataParser.parsePost(Yaml.parse(s"tags: $tags", ZoneId.of("UTC")), s"_posts/$date-$name.html", s"$date-$name.html", ZoneId.of("UTC"))
    val blog = new Blog(BlogInfo("t", None, "a", "en"), List(
      post("2020-01-05", "a", "x"), post("2021-12-31", "b", "y"), post("2021-12-31", "c", "x"), post("2021-02-01", "d", "x")), Nil)
    assert(blog.years.map(_.year) == List(2021, 2020))
    assert(blog.months.map(m => (m.year, m.month)) == List((2021, 12), (2021, 2), (2020, 1)))
    assert(blog.days.map(_.day) == List(31, 1, 5))
    assert(blog.posts.map(_.permalinkTitle) == List("c", "b", "d", "a"))
    assert(blog.tags.keys.toList == List("x", "y"))
    assert(blog.tags("x").map(_.permalinkTitle) == List("c", "d", "a"))
  }

  test("feed makes relative urls absolute") {
    val html = """<a href="2010/a.html">a</a><img src='./images/x.png'/><a href="https://x.com/">b</a><a href="/c">c</a><a href="#d">d</a>"""
    assert(FeedFormatter.absolutify(html, "https://e.com/") ==
      """<a href="https://e.com/2010/a.html">a</a><img src='https://e.com/images/x.png'/><a href="https://x.com/">b</a><a href="/c">c</a><a href="#d">d</a>""")
  }
}

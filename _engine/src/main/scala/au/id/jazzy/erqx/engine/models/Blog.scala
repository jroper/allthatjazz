package au.id.jazzy.erqx.engine.models

import java.time.{ZoneId, ZonedDateTime}

import scala.collection.immutable.ListMap

/** Information about a blog */
case class BlogInfo(title: String,
                    subTitle: Option[String],
                    author: String,
                    language: String,
                    description: Option[String] = None,
                    footer: Option[String] = None,
                    timezone: ZoneId = ZoneId.systemDefault(),
                    properties: Yaml = Yaml.empty)

/**
 * A blog
 *
 * @param info The blog info
 * @param blogPosts The blog posts, in any order
 * @param pages The pages
 */
final class Blog(val info: BlogInfo, blogPosts: List[BlogPost], val pages: List[Page]) {

  /** Blog posts are always listed in reverse chronological order. Posts on the same day are in reverse path order. */
  val posts: List[BlogPost] = blogPosts.sortBy(p => (p.date.toEpochSecond, p.path)).reverse

  /** Blog posts by year, month then day, all in reverse chronological order. */
  val years: List[Year] = {
    // groupBy is stable, so posts within each group stay sorted, but we have to sort the groups themselves
    posts.groupBy(_.date.getYear).toList.sortBy(-_._1).map { case (year, yearPosts) =>
      val months = yearPosts.groupBy(_.date.getMonthValue).toList.sortBy(-_._1).map { case (month, monthPosts) =>
        val days = monthPosts.groupBy(_.date.getDayOfMonth).toList.sortBy(-_._1).map { case (day, dayPosts) =>
          Day(year, month, day, dayPosts)
        }
        Month(year, month, days, monthPosts)
      }
      Year(year, months, yearPosts)
    }
  }

  /** All months that have posts, in reverse chronological order. */
  val months: List[Month] = years.flatMap(_.months)

  /** All days that have posts, in reverse chronological order. */
  val days: List[Day] = months.flatMap(_.days)

  /** Blog posts by tag, ordered alphabetically by tag, posts in reverse chronological order. */
  val tags: ListMap[String, List[BlogPost]] = {
    val byTag = posts.flatMap(post => post.tags.map(_ -> post)).groupBy(_._1).map(kv => kv._1 -> kv._2.map(_._2))
    ListMap(byTag.toSeq.sortBy(_._1)(Blog.tagOrdering): _*)
  }

  /** When the blog was last updated, which is the date of the most recent post. */
  val lastUpdated: ZonedDateTime = posts.headOption.map(_.date).getOrElse(ZonedDateTime.now(info.timezone))

  private val pagesByPermalink: Map[String, Page] = pages.map(page => page.permalink -> page).toMap
  def pageForPermalink(permalink: String): Option[Page] = pagesByPermalink.get(permalink)
}

object Blog {
  /** Case insensitive, falling back to case sensitive to be deterministic */
  val tagOrdering: Ordering[String] =
    Ordering.by[String, String](_.toLowerCase(java.util.Locale.ROOT)).orElse(Ordering.String)
}

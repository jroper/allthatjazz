package au.id.jazzy.erqx.engine.site

import au.id.jazzy.erqx.engine.models.BlogPost

case class Link(text: String, url: String)

/** A group of posts in a list, optionally with a heading that links to somewhere. */
case class PostGroup(heading: Option[Link], posts: List[BlogPost])

/** Links to other lists, to show at the top and bottom of a list. */
case class ListNav(up: Option[Link] = None, previous: Option[Link] = None, next: Option[Link] = None) {
  def isEmpty: Boolean = up.isEmpty && previous.isEmpty && next.isEmpty
  def nonEmpty: Boolean = !isEmpty
}

object ListNav {
  val empty: ListNav = ListNav()
}

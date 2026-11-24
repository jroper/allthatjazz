package au.id.jazzy.erqx.engine.models

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** A day that has published blog posts */
final case class Day(year: Int, month: Int, day: Int, posts: List[BlogPost])

/** A month that has published blog posts. Days are in reverse chronological order. */
final case class Month(year: Int, month: Int, days: List[Day], posts: List[BlogPost]) {
  def name: String = Month.nameOf(month)
  /** The name with the year, eg "January 2026" */
  def fullName: String = s"$name $year"
}

object Month {
  def nameOf(month: Int): String = YearMonth.of(2000, month).getMonth.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
}

/** A year that has published blog posts. Months are in reverse chronological order. */
final case class Year(year: Int, months: List[Month], posts: List[BlogPost])

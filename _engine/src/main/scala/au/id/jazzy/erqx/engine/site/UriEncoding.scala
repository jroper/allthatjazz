package au.id.jazzy.erqx.engine.site

import java.net.URI
import java.nio.charset.StandardCharsets

object UriEncoding {
  // RFC 3986 pchar, minus the sub-delims that are awkward in file names
  private val safe: Set[Char] =
    (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "-._~!$&'()*+,;=:@").toSet

  /** Percent encode a single path segment as UTF-8. */
  def encodePathSegment(segment: String): String = {
    val sb = new StringBuilder
    segment.foreach {
      case c if safe(c) => sb.append(c)
      case c => c.toString.getBytes(StandardCharsets.UTF_8).foreach(b => sb.append("%%%02X".format(b & 0xff)))
    }
    sb.toString
  }

  /** Decode a percent encoded URI path. */
  def decodePath(path: String): String = new URI(path).getPath
}

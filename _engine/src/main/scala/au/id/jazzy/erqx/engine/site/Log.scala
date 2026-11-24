package au.id.jazzy.erqx.engine.site

/** Minimal logging, all output goes to stderr so stdout stays clean. */
object Log {
  def info(message: String): Unit = println(message)

  def warn(message: String, t: Throwable = null): Unit = {
    System.err.println(s"WARNING: $message")
    if (t != null) t.printStackTrace()
  }
}

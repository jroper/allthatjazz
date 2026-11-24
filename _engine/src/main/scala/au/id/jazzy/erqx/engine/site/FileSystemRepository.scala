package au.id.jazzy.erqx.engine.site

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.Files

import play.doc.{FileHandle, FileRepository}

import scala.jdk.CollectionConverters._

/** A play-doc file repository that reads from the file system, under the given base directory. */
class FileSystemRepository(base: File) extends FileRepository {

  private def file(path: String): Option[File] = {
    val f = new File(base, path.dropWhile(_ == '/')).getCanonicalFile
    if (f.toPath.startsWith(base.getCanonicalFile.toPath) && f.isFile) Some(f) else None
  }

  def loadFile[A](path: String)(loader: InputStream => A): Option[A] = file(path).map { f =>
    val is = new FileInputStream(f)
    try loader(is) finally is.close()
  }

  def handleFile[A](path: String)(handler: FileHandle => A): Option[A] = file(path).map { f =>
    val is = new FileInputStream(f)
    handler(FileHandle(f.getName, f.length(), is, () => is.close()))
  }

  def findFileWithName(name: String): Option[String] = {
    val stream = Files.walk(base.toPath)
    try {
      stream.iterator().asScala.find(p => Files.isRegularFile(p) && p.getFileName.toString == name)
        .map(p => base.toPath.relativize(p).toString)
    } finally stream.close()
  }
}

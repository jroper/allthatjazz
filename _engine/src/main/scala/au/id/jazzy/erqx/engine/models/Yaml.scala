package au.id.jazzy.erqx.engine.models

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import au.id.jazzy.erqx.engine.site.Log

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.control.NonFatal

case class Yaml(map: Map[String, AnyRef]) {

  def getString(key: String): Option[String] = getAs[String](key)
  def getDate(key: String): Option[ZonedDateTime] = getAs[ZonedDateTime](key)

  def getAs[T](key: String)(implicit ct: ClassTag[T]): Option[T] = map.get(key).flatMap {
    case t if ct.runtimeClass.isInstance(t) => Some(t.asInstanceOf[T])
    case other =>
      Log.warn(s"Ignoring value for key $key, expected $ct but was $other")
      None
  }
}

object Yaml {

  val empty: Yaml = Yaml(Map())

  def parse(yaml: String, timezone: ZoneId): Yaml = {

    def yamlToScala(obj: AnyRef): AnyRef = obj match {
      case map: java.util.Map[String, AnyRef] @unchecked => new Yaml(map.asScala.toMap.map(kv => kv._1 -> yamlToScala(kv._2)))
      case list: java.util.List[AnyRef] @unchecked => list.asScala.toList.map(yamlToScala)
      case s: String => s
      case n: Number => n
      case b: java.lang.Boolean => b
      case d: Date => ZonedDateTime.ofInstant(d.toInstant, timezone)
      case null => null
      case other =>
        Log.warn(s"Unexpected YAML object of type ${other.getClass}")
        other.toString
    }

    try {
      yamlToScala(new org.yaml.snakeyaml.Yaml().load[AnyRef](yaml)) match {
        case y: Yaml => y
        case other =>
          Log.warn(s"YAML was not object: $other")
          Yaml.empty
      }
    } catch {
      case NonFatal(t) =>
        Log.warn("Error parsing YAML content", t)
        Yaml.empty
    }
  }
}

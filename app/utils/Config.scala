package utils

import org.yaml.snakeyaml.Yaml
import scala.io.Source
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import java.util.LinkedHashMap

class Config() {
  val configFilename: String = "conf/config.yml"
  private var configMap = Map[String, String]()

  def apiKey1() = { configGetter("api-key1") }
  def apiKey2() = { configGetter("api-key2") }

  private def configGetter(key: String): String = {
    configMap get key match {
      case Some(value) => value
      case None =>
        println(s"WARNING: Yaml key '$key' was not loaded from $configFilename")
        ""
    }
  }
}

object Config {
  def apply() = {
    val config = new Config()
    
    try {
      // Read file into string
      val configContents = Source.fromFile(config.configFilename).mkString

      // Build new Yaml instance and load into Scala Map
      val yaml = new Yaml()
      config.configMap = mapAsScalaMap(
                           yaml.load(configContents)
                             .asInstanceOf[LinkedHashMap[String, String]])
    } catch {
      case e: Exception =>
        println(s"ERROR: Could not read config file ${config.configFilename}")
        throw e
    }
    config
  }
}

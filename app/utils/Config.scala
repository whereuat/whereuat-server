package utils

import org.yaml.snakeyaml.Yaml
import scala.io.Source
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import java.util.LinkedHashMap

class Config() {
  private var api_key = ""

  def apiKey() = { api_key }
}

object Config {
  def apply() = {
    val config_fn = "conf/config.yml"
    val config = new Config()
    
    try {
      // Read file into string
      val config_contents = Source.fromFile(config_fn).getLines.mkString

      // Build new Yaml instance and load into Scala Map
      val yaml = new Yaml()
      val config_map = mapAsScalaMap(
                        yaml.load(config_contents)
                            .asInstanceOf[LinkedHashMap[String,String]])

      // Set config instance properties
      config.api_key = config_map get "api-key" match {
        case Some(key) => key
        case None => 
          println("WARNING: Yaml key \"api-key\" was not loaded from config.yml")
          ""
      }
    } catch {
      case e: Exception =>
        println(s"ERROR: Could not read config file $config_fn")
        throw e
    }
    config
  }
}

package utils

import scala.io.Source
import scala.collection.JavaConversions._
import scala.collection.mutable.Map

import java.util.LinkedHashMap

import org.yaml.snakeyaml.Yaml

// Utility singleton to read and store the values from the config file
object Config {
  private val configFilename: String = "conf/config.yml"
  private var configMap = Map[String, String]()

  {
    try {
      // Read file into string
      val configContents = Source.fromFile(configFilename).mkString

      // Build new Yaml instance and load into Scala Map
      val yaml = new Yaml()
      configMap = mapAsScalaMap(
                    yaml.load(configContents)
                      .asInstanceOf[LinkedHashMap[String, String]])
    } catch {
      case e: Exception =>
        println(s"ERROR: Could not read config file ${configFilename}")
        throw e
    }
  }

  // High-level getters
  def twilioAccountSid() = { configGetter("twilio-account-sid") }
  def twilioAuthToken() = { configGetter("twilio-auth-token") }
  def twilioNumber() = { configGetter("twilio-number") }

  def googleApiKey() = { configGetter("google-api-key") }

  // Workhorse method to get a specific config variable from its key
  private def configGetter(key: String): String = {
    configMap get key match {
      case Some(value) => value
      case None =>
        println(s"WARNING: Yaml key '$key' was not loaded from $configFilename")
        ""
    }
  }

}

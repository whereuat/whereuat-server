import play.api._

import com.mongodb.casbah.Imports._

import utils.Config

object Global extends GlobalSettings {
  // Override onStart to create the TTL index on the verifiers collection
  override def onStart(app: Application) {
    val key = MongoDBObject("created-at" -> 1)
    val option = MongoDBObject("expireAfterSeconds" -> 300)
    global.db("verifiers").createIndex(key, option)
  }
}

// global object to store app globals
package object global {
  // App utility globals
  val config = Config
  val db = MongoClient("localhost", 27017)("whereu@")

  // GCM-related constants
  val GCM_RETRIES = 5
  val GCM_TYPE_REQUEST = "AT_REQUEST"
  val GCM_TYPE_RESPONSE = "AT_RESPONSE"

  // JSON argument globals
  val OS_IOS = "IOS"
  val OS_ANDROID = "ANDROID"

  // Miscellaneous globals
  val EARTH_RADIUS = 6371
}

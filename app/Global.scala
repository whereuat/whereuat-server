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

package object global {
  val config = Config
  val db = MongoClient("localhost", 27017)("whereu@")

  val GCM_RETRIES = 5
  val GCM_TYPE_REQUEST = "AT_REQUEST"
  val GCM_TYPE_RESPONSE = "AT_RESPONSE"

  val OS_IOS = "IOS"
  val OS_ANDROID = "ANDROID"
  val EARTH_RADIUS = 6371
}

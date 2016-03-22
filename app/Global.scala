import play.api._

import com.mongodb.casbah.Imports._

import utils.Config

object Global extends GlobalSettings {
  // Override onStart to create the TTL index on the verifiers collection
  override def onStart(app: Application) {
    val db = MongoClient("localhost", 27017)("whereu@")
    val key = MongoDBObject("created-at" -> 1)
    val option = MongoDBObject("expireAfterSeconds" -> 300)
    db("verifiers").createIndex(key, option)
  }
}

package object global {
  val config = Config
  val GCM_RETRIES = 5
}

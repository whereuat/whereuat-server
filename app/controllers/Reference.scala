package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON._
import com.google.android.gcm.server.Sender
import com.google.android.gcm.server.Message

class Reference extends Controller {
  val db = MongoClient("localhost", 27017)("test")

  // Database Query Reference route
  def dbQuery = Action {
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList
    
    Ok(s"Test collection query:\n${serialize(list)}")
  }

  
  // Sender for GCM messages
  val gcmSender = new Sender(global.config.gcmApiKey)

  // JSON Reads for GCM routes
  val gcmTokenReads : Reads[String] = (
    (JsPath \ "gcm-token").read[String]
  )

  val gcmMsgReads : Reads[String] = (
    (JsPath \ "message").read[String]
  )

  // GCM Setup Reference routes
  // Parse client's GCM token
  def gcmToken = Action(parse.json) { request =>
    request.body.validate(gcmTokenReads).map {
      case (gcmTok) =>
        val coll = db("clients")
        coll.drop()
        val client = MongoDBObject({"gcm-token" -> gcmTok})
        coll.insert(client)
        Ok("POST to /ref/gcmtoken received")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  // Send notification to client
  def gcmNotify = Action(parse.json) { request =>
    request.body.validate(gcmMsgReads).map {
      case (gcmMsg) =>
        // Get a client from the database
        val gcmTok: String = db("clients").findOne.get("gcm-token") match {
          case tok: String => tok
          case None => ""
        }
        // Build GCM message
        val msg = new Message.Builder()
          .addData("message", gcmMsg)
          .build()
        // Send message
        gcmSender.send(msg, gcmTok, 5)
        Ok("GCM message sent")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }
}

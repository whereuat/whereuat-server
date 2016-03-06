package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON._
import com.google.android.gcm.server.Sender

class Reference extends Controller {
  // Accessing global config values
  println(global.config.apiKey1)
  
  val db = MongoClient("localhost", 27017)("test")

  // Database Query Reference route
  def dbQuery = Action {
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList
    
    Ok(s"Test collection query:\n${serialize(list)}")
  }


  val gcmSender = new Sender(config.gcmApiKey)

  val gcmTokenReads : Reads[String] = (
    (JsPath \ "gcm-token").read[String]
  )

  // GCM Setup Reference routes
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

  def showClients = Action {
    val coll = db("clients")
    Ok(s"Clients:\n${serialize(coll.find().toList)}")
  }

  def gcmNotify = Action {
    Ok("POST to /ref/gcmsend received")
  }
}

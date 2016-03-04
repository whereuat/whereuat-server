package controllers

import play.api._
import play.api.mvc._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON._

class Reference extends Controller {
  // Database Query Reference route
  val dbQuery = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList
    
    Ok(s"Test collection query:\n${serialize(list)}")
  }

  // GCM Setup Reference routes
  val gcmToken = Action {
    Ok("POST to /ref/gcmtoken received")
  }

  val gcmNotify = Action {
    Ok("POST to /ref/gcmsend received")
  }
}

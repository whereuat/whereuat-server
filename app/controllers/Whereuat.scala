package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON._

class Whereuat extends Controller {
  // Case classes for JsValues
  case class Location(latitude: Double, longitude: Double)
  case class KeyLocation(name: String, location: Location)


  // Implicit Reads for case classes
  implicit val locationReads : Reads[Location] = (
    (JsPath \ "latitude").read[Double] and
    (JsPath \ "longitude").read[Double]
  )(Location.apply _)

  implicit val keyLocationReads : Reads[KeyLocation] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "location").read[Location]
  )(KeyLocation.apply _)


  // Explicit Reads
  val phoneReads : Reads[String] = (
    (JsPath \ "phone-#").read[String]
  )

  val gcmReads : Reads[String] = (
    (JsPath \ "gcm-id").read[String]
  )

  val verifyReads : Reads[String] = (
    (JsPath \ "verification-code").read[String]
  )


  // Request actions
  def requestAccount = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(serialize(list))
  }

  def createAccount = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(serialize(list))
  }

  def request = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(serialize(list))
  }

  def respond = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(serialize(list))
  }

}

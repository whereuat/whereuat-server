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
  val requestReads : Reads[String] = (
    (JsPath \ "phone-#").read[String]
  )

  val createReads : Reads[(String, String, String)] = (
    (JsPath \ "phone-#").read[String] and
    (JsPath \ "gcm-id").read[String] and
    (JsPath \ "verification-code").read[String]
  ) tupled

  val whereReads : Reads[(String, String)] = (
    (JsPath \ "from" \ "phone-#").read[String] and
    (JsPath \ "to" \ "phone-#").read[String]
  ) tupled

  val atReads : Reads[(String, String, Location, Seq[KeyLocation])] = (
    (JsPath \ "from" \ "phone-#").read[String] and
    (JsPath \ "to" \ "phone-#").read[String] and
    (JsPath \ "location").read[Location] and
    (JsPath \ "key-locations").read[Seq[KeyLocation]]
  ) tupled


  // Route actions
  def requestAccount = Action(parse.json) { request =>
    request.body.validate(requestReads).map {
      case (phone) =>
        val mongoClient = MongoClient("localhost", 27017)
        val db = mongoClient("test")
        val coll = db("test")

        val docs = coll.find()
        val list = docs.toList

        Ok("Requested account's phone number: " + phone + "\n\n" + 
           "Test directory query:\n" + serialize(list))
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toFlatJson(e))
    }
  }

  def createAccount = Action(parse.json) { request =>
    request.body.validate(createReads).map {
      case (phone, gcm, vcode) =>
        val mongoClient = MongoClient("localhost", 27017)
        val db = mongoClient("test")
        val coll = db("test")

        val docs = coll.find()
        val list = docs.toList

        Ok("Created account's phone number: " + phone + "\n" +
           "Created account's GCM ID: " + gcm + "\n" +
           "Created account's verification code: " + vcode + "\n\n" + 
           "Test directory query:\n" + serialize(list))
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toFlatJson(e))
    }
  }

  def atRequest = Action(parse.json) { request =>
    request.body.validate(whereReads).map {
      case (from, to) =>
        val mongoClient = MongoClient("localhost", 27017)
        val db = mongoClient("test")
        val coll = db("test")

        val docs = coll.find()
        val list = docs.toList

        Ok("@ Request's from phone number: " + from + "\n" +
           "@ Request's to phone number: " + to + "\n\n" +
           "Test directory query:\n" + serialize(list))
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toFlatJson(e))
    }
  }

  def atRespond = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(serialize(list))
  }

}

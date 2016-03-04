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
  case class Coordinates(latitude: Double, longitude: Double)
  case class Location(name: Option[String], location: Coordinates)


  // Implicit Reads for case classes
  implicit val coordReads : Reads[Coordinates] = (
    (JsPath \ "latitude").read[Double] and
    (JsPath \ "longitude").read[Double]
  )(Coordinates.apply _)

  implicit val locationReads : Reads[Location] = (
    (JsPath \ "name").readNullable[String] and
    (JsPath \ "coordinates").read[Coordinates]
  )(Location.apply _)


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
    (JsPath \ "from").read[String] and
    (JsPath \ "to").read[String]
  ) tupled

  val atReads : Reads[(String, String, Location)] = (
    (JsPath \ "from").read[String] and
    (JsPath \ "to").read[String] and
    (JsPath \ "location").read[Location]
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

        Ok(s"Requested account's phone number: $phone\n\n" + 
           s"Test directory query:\n${serialize(list)}")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
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

        Ok(s"Created account's phone number: $phone\n" +
           s"Created account's GCM ID: $gcm\n" +
           s"Created account's verification code: $vcode\n\n" + 
           s"Test directory query:\n${serialize(list)}")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
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

        Ok(s"@ Request's from phone number: $from\n" +
           s"@ Request's to phone number: $to\n\n" +
           s"Test directory query:\n${serialize(list)}")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  def atRespond = Action(parse.json) { request =>
    request.body.validate(atReads).map {
      case (from, to, loc) =>
        val mongoClient = MongoClient("localhost", 27017)
        val db = mongoClient("test")
        val coll = db("test")

        val docs = coll.find()
        val list = docs.toList

        Ok(s"@ Response's from phone number: $from\n" +
           s"@ Response's to phone number: $to\n" +
           s"@ Response's location: " + 
             (if (loc.name.isDefined) loc.name.get + " " else "") + 
           f"(${loc.location.latitude}%2.7f,${loc.location.longitude}%2.7f)\n\n" +
           s"Test directory query:\n${serialize(list)}")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

}

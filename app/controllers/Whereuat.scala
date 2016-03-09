package controllers

// Play imports
import play.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.mvc._

// Scala imports
import scala.util.Random

// Java imports
import java.util.Date

// Third-party imports
import com.google.android.gcm.server.{Sender, Message}
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON._

// whereu@ imports
import utils.SmsVerificationSender

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
    (JsPath \ "gcm-token").read[String] and
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


  // Controller-scope values
  val db = MongoClient("localhost", 27017)("whereu@")
  val gcmSender = new Sender(global.config.gcmApiKey)


  // Utility functions
  def phoneToGcm(phone: String) : String = {
    val query = MongoDBObject("phone-#" -> phone)
    val gcmTok: String = db("clients").findOne(query).get("gcm-token") match {
      case tok: String => tok
      case None => ""
    }
    gcmTok
  }

  // Creates a random string of 5 integers.
  def genVerificationCode(): String = {
    (for (x <- 1 to 5) yield Random.nextInt(10)) take 5 mkString
  }

  // Returns true if the the phone number _phone_ is in the verifiers collection
  // and the verification code _vCode_ matches the one in the collection.
  def isVerified(phone: String, vCode: String): Boolean = {
    val query = MongoDBObject("phone-#" -> phone, "verification-code" -> vCode)
    db("verifiers").findOne(query) != None
  }


  // Route actions
  // POST route for when a client wants to initially create an account. This
  // request should receive the client's properly formatted phone number in the
  // request body, create a verification code and add it to the verifiers
  // collection, and then send a text to the client's phone number.
  def requestAccount = Action(parse.json) { request =>
    request.body.validate(requestReads).map {
      case (phone) =>
        val query = MongoDBObject("phone-#" -> phone)
        val vCode = genVerificationCode()
        val verifier = MongoDBObject("phone-#" -> phone,
                                     "verification-code" -> vCode,
                                     "created-at" -> new Date())
        // Update with an upsert because the verifier should overwrite an
        // existing one for the same phone number and create a new one if one
        // doesn't exist.
        db("verifiers").update(query, verifier, upsert=true)

        SmsVerificationSender.send(phone, vCode)
        Ok(s"Created verifier for phone number $phone")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  // POST route for when a user sends their verification code to create their
  // account. This request should receive the client's properly formatted phone
  // number, GCM ID, and verification code in the request body, make sure the
  // verification code matches the one stored in the verifiers collection, and
  // then add them to the clients collection if it does.
  def createAccount = Action(parse.json) { request =>
    request.body.validate(createReads).map {
      case (phone, gcm, vCode) =>
        if (isVerified(phone, vCode)) {
          val query = MongoDBObject("phone-#" -> phone)
          val client = MongoDBObject("gcm-token" -> gcm, "phone-#" -> phone)
          // Update with an upsert rather than insert in order to handle a
          // client needing to create their account.
          db("client").update(query, client, upsert=true)
          Ok(s"Created account for phone number $phone\n")
        } else {
          InternalServerError("VERIFICATION ERROR: Verification codes do not " +
                              "match\n")
        }
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  def atRequest = Action(parse.json) { request =>
    request.body.validate(whereReads).map {
      case (from, to) =>
        val toGcmTok = phoneToGcm(to)
        val msg = new Message.Builder()
          .addData("message", s"$from has sent you an @request")
          .build()
        gcmSender.send(msg, toGcmTok, global.GCM_RETRIES)
        Ok(s"@ Request's from phone number: $from\n" +
           s"@ Request's to phone number: $to")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  def atRespond = Action(parse.json) { request =>
    request.body.validate(atReads).map {
      case (from, to, loc) =>
        val name_str = if (loc.name.isDefined) loc.name.get + " " else ""
        val lat_str = f"${loc.location.latitude}%2.7f"
        val long_str = f"${loc.location.longitude}%2.7f"

        val toGcmTok = phoneToGcm(to)
        val msg = new Message.Builder()
          .addData("message", s"$from has responded to your @request")
          .build()
        gcmSender.send(msg, toGcmTok, global.GCM_RETRIES)
        Ok(s"@ Response's from phone number: $from\n" +
           s"@ Response's to phone number: $to\n" +
           s"@ Response's location: $name_str($lat_str,$long_str)")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

}

package controllers

// Play imports
import play.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

// Scala imports
import scala.util.Random

// Java imports
import java.util.Date

// Third-party imports
import com.mongodb.casbah.Imports._

// whereu@ imports
import utils.LocationFinder
import utils.LocationFinder.{Location, Place}
import utils.SmsVerificationSender
import utils.GcmSender


class Whereuat extends Controller {
  // Explicit Reads
  val requestReads : Reads[String] = (
    (JsPath \ "phone-#").read[String]
  )

  val createReads : Reads[(String, String, String, String)] = (
    (JsPath \ "phone-#").read[String] and
    (JsPath \ "gcm-token").read[String] and
    (JsPath \ "verification-code").read[String] and
    (JsPath \ "client-os").read[String]
  ) tupled

  val whereReads : Reads[(String, String)] = (
    (JsPath \ "from").read[String] and
    (JsPath \ "to").read[String]
  ) tupled

  val atReads : Reads[(String, String, Location, Option[Place])] = (
    (JsPath \ "from").read[String] and
    (JsPath \ "to").read[String] and
    (JsPath \ "current-location").read[Location] and
    // Client also sends the nearest key location to the server. If none exists
    // then the client sends a null.
    (JsPath \ "key-location").readNullable[Place]
  ) tupled


  // Utility functions
  // Creates a random string of 5 integers.
  def genVerificationCode(): String = {
    (for (x <- 1 to 5) yield Random.nextInt(10)) take 5 mkString
  }

  // Returns true if the the phone number _phone_ is in the verifiers collection
  // and the verification code _vCode_ matches the one in the collection.
  def isVerified(phone: String, vCode: String): Boolean = {
    val query = MongoDBObject("phone-#" -> phone, "verification-code" -> vCode)
    global.db("verifiers").findOne(query) != None
  }

  // Returns true if the given OS _os_ is a valid OS type
  def isValidOs(os: String): Boolean = {
    os == global.OS_IOS || os == global.OS_ANDROID
  }


  // Route actions
  def index = Action {
    Ok(views.html.index())
  }

  def team = Action {
    Ok(views.html.team())
  }

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
        global.db("verifiers").update(query, verifier, upsert=true)

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
      case (phone, gcm, vCode, os) =>
        val verified = isVerified(phone, vCode)
        val valid_os = isValidOs(os)
        if (verified && valid_os) {
          val query = MongoDBObject("phone-#" -> phone)
          val client = MongoDBObject("gcm-token" -> gcm, 
                                     "phone-#" -> phone, 
                                     "client-os" -> os)
          // Update with an upsert rather than insert in order to handle a
          // client needing to create their account.
          global.db("clients").update(query, client, upsert=true)
          global.db("verifiers").remove(query)
          Ok(s"Created account for phone number $phone\n")
        } else if (!verified) {
          InternalServerError("VERIFICATION ERROR: Verification codes do not " +
                              "match\n")
        } else {
          BadRequest("JSON ERROR: Invalid OS\n")
        }
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  // POST route for when a user sends an @request to another user. This route
  // should receive the client's properly formatted phone number and the phone
  // number of their desired recipient, verify that the recipient exists in the
  // database, then send the recipient a push notification
  def atRequest = Action(parse.json) { request =>
    request.body.validate(whereReads).map {
      case (from, to) =>
        try {
          GcmSender.sendAtRequestNotif(from, to)
          Ok(s"@ Request's from phone number: $from\n" +
             s"@ Request's to phone number: $to")
        } catch {
          case e: GcmSender.TokenNotFoundException =>
            UnprocessableEntity(s"ERROR: $to not found in database")
          case e: GcmSender.GcmSendFailedException =>
            FailedDependency("ERROR: GCM notification send failed")
        }
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  // POST route for when a user responds to another user's @request. This route
  // should receive the client's properly formatted phone number, current
  // GPS coordinates, closest key location, and phone number of their desired
  // recipient, then verify that the recipient exists in the database, and
  // send the recipient a push notification
  def atRespond = Action(parse.json) { request =>
    request.body.validate(atReads).map {
      case (from, to, currLoc, keyLoc) =>
        val latStr = f"${currLoc.lat}%2.7f"
        val longStr = f"${currLoc.lng}%2.7f"
        LocationFinder.nearestLocation(currLoc, keyLoc) match {
          case None =>
            FailedDependency("ERROR: " +
              s"Nearest location for ($latStr,$longStr) could not be found")
          case Some(nearLoc) =>
            try {
              GcmSender.sendAtRespondNotif(from, to, nearLoc)
              Ok(s"@ Response's from phone number: $from\n" +
                 s"@ Response's to phone number: $to\n" +
                 s"@ Response's location: ${nearLoc.name}")
            } catch {
              case e: GcmSender.TokenNotFoundException =>
                UnprocessableEntity(s"ERROR: $to not found in database")
              case e: GcmSender.GcmSendFailedException =>
                FailedDependency("ERROR: GCM notification send failed")
            }
        }
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

}

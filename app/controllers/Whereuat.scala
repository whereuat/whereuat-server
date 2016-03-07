package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

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
        val sender = new SmsVerificationSender()
        sender.send(phone, "Input 12345 into whereu@ to create your account.")
        Ok(s"Requested account's phone number: $phone")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  def createAccount = Action(parse.json) { request =>
    request.body.validate(createReads).map {
      case (phone, gcm, vcode) =>
        Ok(s"Created account's phone number: $phone\n" +
           s"Created account's GCM ID: $gcm\n" +
           s"Created account's verification code: $vcode")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

  def atRequest = Action(parse.json) { request =>
    request.body.validate(whereReads).map {
      case (from, to) =>
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
        Ok(s"@ Response's from phone number: $from\n" +
           s"@ Response's to phone number: $to\n" +
           s"@ Response's location: $name_str($lat_str,$long_str)")
    }.recoverTotal {
      e => BadRequest("ERROR: " + JsError.toJson(e))
    }
  }

}

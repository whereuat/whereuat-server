package utils

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

import scala.concurrent.Future
import scala.util.{Success, Failure}

object LocationFinder {
  case class Location(lat: Double, lng: Double)
  case class Place(name: Option[String], location: Location)
  case class Places(places: Seq[Place])


  // Implicit Reads for case classes
  implicit val locationReads : Reads[Location] = (
    (JsPath \ "lat").read[Double] and
    (JsPath \ "lng").read[Double]
  )(Location.apply _)

  implicit val placeReads : Reads[Place] = (
    (JsPath \ "name").readNullable[String] and
    (JsPath \ "geometry" \ "location").read[Location]
  )(Place.apply _)

  val placesReads : Reads[Places] = (
    (JsPath \ "results").read[Seq[Place]].map(Places(_))
  )


  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def nearestLocation(currLoc: Location, 
                      keyLoc: Option[Place] = None): Place = {
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    val locString = f"${currLoc.lat}%2.6f,${currLoc.lng}%2.6f"
    val response = WS.url(url)
      .withQueryString("key" -> global.config.googleApiKey)
      .withQueryString("location" -> locString)
      .withQueryString("rankby" -> "distance")
      .withQueryString("type" -> "establishment")
      .get()
    val nearest: Future[Place] = response.map { res =>
      res.json.validate(placesReads).map {
        case p =>
          p.places(0)
      }.recoverTotal {
        e => Place(None, Location(0, 0))
      }
    }
    nearest.onComplete {
      case Success(p) =>
        println(p.toString)
      case Failure(ex) =>
        println("ERROR: " + ex)
    }

    var ret = Place(None, Location(1.0, 1.0))
    ret
  }
}

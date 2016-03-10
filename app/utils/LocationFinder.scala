package utils

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.math._
import scala.util.{Success, Failure}

object LocationFinder {
  case class Location(lat: Double, lng: Double)
  case class Place(name: String, location: Location)
  case class Places(places: Seq[Place])


  // Implicit Reads for case classes
  implicit val locationReads : Reads[Location] = (
    (JsPath \ "lat").read[Double] and
    (JsPath \ "lng").read[Double]
  )(Location.apply _)

  implicit val placeReads : Reads[Place] = (
    (JsPath \ "name").read[String] and
    (JsPath \ "geometry" \ "location").read[Location]
  )(Place.apply _)

  val placesReads : Reads[Places] = (
    (JsPath \ "results").read[Seq[Place]].map(Places(_))
  )


  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def dist(a: Location, b: Location): Double = {
    sqrt(pow(a.lat - b.lat, 2) + pow(a.lng - b.lng, 2))
  }

  def nearestLocation(currLoc: Location, 
                      keyLoc: Option[Place] = None): Option[Place] = {
    val placeLoc = nearestPlacesLocation(currLoc)
    var placeDist = Double.MaxValue
    if (placeLoc isDefined) {
      placeDist = dist(currLoc, placeLoc.get.location)
    }

    var keyDist = Double.MaxValue
    if (keyLoc isDefined) {
      keyDist = dist(currLoc, keyLoc.get.location)
    }

    if (keyDist == Double.MaxValue && placeDist == Double.MaxValue) {
      None
    } else if (keyDist <= placeDist) {
      keyLoc
    } else {
      placeLoc
    }
  }

  def nearestPlacesLocation(currLoc: Location): Option[Place] = {
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    val locString = f"${currLoc.lat}%2.6f,${currLoc.lng}%2.6f"
    val response = WS.url(url)
      .withQueryString("key" -> global.config.googleApiKey)
      .withQueryString("location" -> locString)
      .withQueryString("rankby" -> "distance")
      .withQueryString("type" -> "establishment")
      .get()
    val nearestFuture: Future[Option[Place]] = response.map { res =>
      res.json.validate(placesReads).map {
        case p =>
          if (p.places.length > 0) 
            Some(p.places(0))
          else
            None
      }.recoverTotal {
        e => None
      }
    }
    val nearestOpt = Await.ready(nearestFuture, Duration.Inf).value.get match {
      case Success(p) =>
        p
      case Failure(e) =>
        println("ERROR: " + e)
        None
    }
    nearestOpt
  }
}

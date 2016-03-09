package utils

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

import scala.concurrent.Future
import scala.util.{Success, Failure}

object LocationFinder {
  case class Coordinates(latitude: Double, longitude: Double)
  case class Location(name: Option[String], location: Coordinates)

  case class GooglePlace(name: String, location: Coordinates, ptype: String)
  case class GooglePlaces(places: Seq[GooglePlace])

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def nearestLocation(currLoc: Coordinates, 
                      keyLoc: Option[Location] = None): Location = {
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    val locString = f"${currLoc.latitude}%2.6f,${currLoc.longitude}%2.6f"
    val response = WS.url(url)
      .withQueryString("key" -> global.config.googleApiKey)
      .withQueryString("location" -> locString)
      .withQueryString("rankby" -> "distance")
      .get()
    val jsonResult: Future[String] = response.map {
      res =>
        Json.stringify(res.json)
    }
    jsonResult.onComplete {
      case Success(s) =>
        println(s)
      case Failure(ex) =>
        println("ERROR: " + ex)
    }

    var ret = Location(None, Coordinates(1.0, 1.0))
    ret
  }
}

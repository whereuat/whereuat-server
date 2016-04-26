package utils

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Success, Failure}

// Singleton object for finding the nearest location to a point
object LocationFinder {
  // Utility case classes used for pattern matching
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

  // Explicit Reads for Places case class
  val placesReads : Reads[Places] = (
    (JsPath \ "results").read[Seq[Place]].map(Places(_))
  )


  // Concurrent execution context for Future
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global


  // Find haversine distance between two GPS coordinates
  // Haversine is used to find distance over the surface of the Earth
  // approximated as a sphere. A naive Euclidean distance implementation would
  // produce significant error near the poles, even as far south as England,
  // where a degree of longitude is 1.5 times larger than a degree of latitude
  // in terms of distance
  def dist(a: Location, b: Location): Double = {
    val a_lat_rad = Math.toRadians(a.lat)
    val a_lng_rad = Math.toRadians(a.lng)

    val b_lat_rad = Math.toRadians(b.lat)
    val b_lng_rad = Math.toRadians(b.lng)

    val haversine = (theta: Double) => (1 - Math.cos(theta))/2

    2 * global.EARTH_RADIUS * Math.asin(Math.sqrt(
      haversine(a_lat_rad - b_lat_rad) +
      Math.cos(a_lat_rad)*Math.cos(b_lat_rad)*haversine(a_lng_rad - b_lng_rad)
    ))
  }

  // Find the nearest location to the given current location, given the nearest
  // key location
  def nearestLocation(currLoc: Location, 
                      keyLoc: Option[Place] = None): Option[Place] = {
    val placeLoc = nearestPlacesLocation(currLoc)
    var placeDist = if (placeLoc isDefined) dist(currLoc, placeLoc.get.location)
                    else Double.MaxValue
    var keyDist = if (keyLoc isDefined) dist(currLoc, keyLoc.get.location)
                  else Double.MaxValue

    if (keyDist == Double.MaxValue && placeDist == Double.MaxValue) {
      None
    } 
    else if (keyDist <= placeDist) keyLoc else placeLoc
  }

  // Find the nearest location given by the Places API
  def nearestPlacesLocation(currLoc: Location): Option[Place] = {
    // Initiate the GET request to the Places API server and get the response
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
    val locString = f"${currLoc.lat}%2.6f,${currLoc.lng}%2.6f"
    val response = WS.url(url)
      .withQueryString("key" -> global.config.googleApiKey)
      .withQueryString("location" -> locString)
      .withQueryString("rankby" -> "distance")
      .withQueryString("type" -> "establishment")
      .get()

    // Build an Option[Place] from the API server's response
    val nearestFuture: Future[Option[Place]] = response.map { res =>
      res.json.validate(placesReads).map {
        case p =>
          if (p.places.length > 0) Some (p.places(0)) else None
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

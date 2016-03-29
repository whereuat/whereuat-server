import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

import utils.LocationFinder
import utils.LocationFinder.{Location, Place}


class LocationSpec extends Specification {

  "Location" should {

    "compute distance correctly" in {

      "for zero distance" in {
        val loc = Location(0, 0)
        LocationFinder.dist(loc, loc) must be equalTo(0)
      }

      "for non-zero distance" in {
        val loc1 = Location(0, 0)
        val loc2 = Location(41.035232, -73.767067)
        LocationFinder.dist(loc1, loc2) must be equalTo(84.4125016752632)
      }
    }

    "get response from Places API" in {

      "to find a location that exists" in new WithApplication {
        val loc = Location(42.729913, -73.676565)
        val placeName = "Rensselaer Union"

        val place = LocationFinder.nearestPlacesLocation(loc).get
        place.name must be equalTo(placeName)
      }

      "to return None if no place exists" in new WithApplication {
        val loc = Location(-76.245697, 52.503294)
        LocationFinder.nearestPlacesLocation(loc) must be equalTo(None)
      }
    }

    "find nearest location" in {

      "when a key location is not given" in new WithApplication {
        val loc = Location(42.729913, -73.676565)
        val placeName = "Rensselaer Union"

        val place = LocationFinder.nearestLocation(loc).get
        place.name must be equalTo(placeName)
      }

      "when a key location is closer" in new WithApplication {
        val loc = Location(42.729913, -73.676565)
        val keyLocName = "My Key Location"
        val keyLoc = Option(Place(keyLocName, loc))

        val place = LocationFinder.nearestLocation(loc, keyLoc).get
        place.name must be equalTo(keyLocName)
      }

      "when a key location is not closer" in new WithApplication {
        val loc = Location(42.729913, -73.676565)
        val placeName = "Rensselaer Union"
        val keyLocName = "My Key Location"
        val keyLoc = Option(Place(keyLocName, Location(0,0)))

        val place = LocationFinder.nearestLocation(loc, keyLoc).get
        place.name must be equalTo(placeName)
      }
    }
  }
}

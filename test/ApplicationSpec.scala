import org.specs2.mutable._

import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._


class ApplicationSpec extends Specification {
  "Application" should {

    "send 404 on an invalid route" in new WithApplication {
      route(FakeRequest(GET, "/foo")) must beSome.which (status(_) == NOT_FOUND)
    }

    "have POST routes for" in {
      "request account" in new WithApplication {
        route(
          FakeRequest(POST, "/account/request")
        ) must not (beSome.which (status(_) == NOT_FOUND))
      }

      "new account" in new WithApplication {
        route(
          FakeRequest(POST, "/account/new")
        ) must not (beSome.which (status(_) == NOT_FOUND))
      }

      "location request" in new WithApplication {
        route(
          FakeRequest(POST, "/where")
        ) must not (beSome.which (status(_) == NOT_FOUND))
      }

      "location respond" in new WithApplication {
        route(
          FakeRequest(POST, "/at")
        ) must not (beSome.which (status(_) == NOT_FOUND))
      }
    }
  }
}

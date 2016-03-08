import play.api._

import utils.Config

object Global extends GlobalSettings {
  // Global.scala normally overrides default Play framework behavior
  // with this class
}

package object global {
  val config = Config()
  val GCM_RETRIES = 5
}

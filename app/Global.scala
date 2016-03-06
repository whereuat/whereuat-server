import play.api._

import utils.Config

object Global extends GlobalSettings {
}

package object global {
  val config = Config()
}

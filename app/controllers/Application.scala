package controllers

import play.api._
import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def newAccount = Action {
    Ok(views.html.newaccount())
  }

  def at = Action {
    Ok(views.html.at())
  }

  def where = Action {
    Ok(views.html.where())
  }

}

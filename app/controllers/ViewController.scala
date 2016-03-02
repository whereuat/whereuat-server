package controllers

import play.api._
import play.api.mvc._

class ViewController extends Controller {

  def read = Action {
    val mongoClient = MongoClient("localhost", 27017)
    val db = mongoClient("test")
    val coll = db("test")

    val docs = coll.find()
    val list = docs.toList

    Ok(views.html.read(list))
  }

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

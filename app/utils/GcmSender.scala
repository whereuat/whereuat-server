package utils

import java.io.IOException

import play.api.mvc._

import com.google.android.gcm.server.{Sender, Message}
import com.mongodb.casbah.Imports._

import LocationFinder.Place

// Singleton object to send GCM messages in the correct format to the two
// client OS's
object GcmSender {
  private val gcmSender = new Sender(global.config.googleApiKey)

  // Parent trait for implementing a message class
  private trait GcmMsg {
    // Recipient phone number
    var to : String
    // Method for retrieving push notification
    def getMsg(os: String) : Message
  }

  // Sends GCM messages for the atRequest route
  private class AtRequest(var to: String, from: String) extends GcmMsg {
    def getMsg(os: String) : Message = {
      val builder = new Message.Builder()
      if (os == global.OS_IOS) {
        builder.contentAvailable(true)
      }
      builder.addData("type", s"${global.GCM_TYPE_REQUEST}")
             .addData("from-#", s"$from")
             .build()
    }
  }
  private object AtRequest {
    def apply(to: String, from: String) = {
      new AtRequest(to, from)
    }
  }

  // Sends GCM messages for the atRespond route
  private class AtRespond(var to: String, from: String,
                          place: Place) extends GcmMsg {
    def getMsg(os: String) : Message = {
      val builder = new Message.Builder()
      if (os == global.OS_IOS) {
        builder.contentAvailable(true)
      }
      builder.addData("type", s"${global.GCM_TYPE_RESPONSE}")
             .addData("from-#", s"$from")
             .addData("place", s"${place.name}")
             .addData("lat", f"${place.location.lat}%2.6f")
             .addData("lng", f"${place.location.lng}%2.6f")
             .build()
    }
  }
  private object AtRespond {
    def apply(to: String, from: String, place: Place) = {
      new AtRespond(to, from, place)
    }
  }

  case class TokenNotFoundException() extends Exception
  case class GcmSendFailedException() extends Exception

  // Public method to send push notifications for the atRequest route
  def sendAtRequestNotif(from: String, to: String) : Unit = {
    sendNotif(AtRequest(to, from))
  }

  // Public method to send push notifications for the atRespond route
  def sendAtRespondNotif(from: String, to: String, place: Place) : Unit = {
    sendNotif(AtRespond(to, from, place))
  }

  // Workhorse method for sending the push notification
  private def sendNotif(msg: GcmMsg) : Unit = {
    val query = MongoDBObject("phone-#" -> msg.to)
    global.db("clients").findOne(query) match {
      case Some(doc) =>
        val gcmTok = doc.get("gcm-token").toString
        val gcm_msg = msg.getMsg(doc.get("client-os").toString)
        try {
          gcmSender.send(gcm_msg, gcmTok, global.GCM_RETRIES)
        } catch {
          case e: IOException => throw new GcmSendFailedException()
        }
      case None => throw TokenNotFoundException()
    }
  }
}

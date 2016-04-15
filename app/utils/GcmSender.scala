package utils

import play.api.mvc._

import com.google.android.gcm.server.{Sender, Message, Notification}
import com.mongodb.casbah.Imports._

object GcmSender {
  private val db = MongoClient("localhost", 27017)("whereu@")
  private val gcmSender = new Sender(global.config.googleApiKey)

  // Parent trait for implementing a message class
  private trait GcmMsg {
    // Recipient phone number
    var to : String
    // Method for retrieving iOS version of push notification
    def getIosMsg() : Message
    // Method for retrieving Android version of push notification
    def getAndroidMsg() : Message
  }

  // Sends GCM messages for the atRequest route
  private class AtRequest(var to: String, from: String) extends GcmMsg {
    def getIosMsg() : Message = {
      val notification = new Notification.Builder("whereu@")
        .body(s"Location Request from $from")
        .title(s"whereu@")
        .clickAction("REQUEST_LOCATION_CATEGORY")
        .build()
      new Message.Builder()
        .notification(notification)
        .build()
    }

    def getAndroidMsg() : Message = {
      new Message.Builder()
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
  // TODO: Implement this
  private class AtRespond(var to: String, from: String, 
                          place: String) extends GcmMsg {
    def getIosMsg() : Message = {
      new Message.Builder()
        .build()
    }

    def getAndroidMsg() : Message = {
      new Message.Builder()
        .addData("message", s"$from is at $place")
        .build()
    }
  }
  private object AtRespond {
    def apply(to: String, from: String, place: String) = {
      new AtRespond(to, from, place)
    }
  }

  case class TokenNotFoundException() extends Exception

  // Public method to send push notifications for the atRequest route
  def sendAtRequestNotif(from: String, to: String) : Unit = {
    sendNotif(AtRequest(to, from))
  }

  // Public method to send push notifications for the atRespond route
  def sendAtRespondNotif(from: String, to: String, place: String) : Unit = {
    sendNotif(AtRespond(to, from, place))
  }

  // Workhorse method for sending the push notification
  private def sendNotif(msg: GcmMsg) : Unit = {
    val query = MongoDBObject("phone-#" -> msg.to)
    db("clients").findOne(query) match {
      case Some(doc) =>
        val gcmTok = doc.get("gcm-token").toString
        val os = doc.get("client-os").toString

        val gcm_msg = os match {
          case global.OS_IOS =>
            msg.getIosMsg()
          case global.OS_ANDROID =>
            msg.getAndroidMsg()
        }
        gcmSender.send(gcm_msg, gcmTok, global.GCM_RETRIES)
      case None => throw TokenNotFoundException()
    }
  }
}


package controllers

import collection.JavaConversions._

import com.twilio.sdk.resource.factory.MessageFactory
import com.twilio.sdk.resource.instance.{Account, Message}
import com.twilio.sdk.{TwilioRestClient, TwilioRestException}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.NameValuePair


class SmsVerificationSender {
  def send(clientNumber: String, message: String) {
    try {
      var params = List[NameValuePair]()
      params ::= (new BasicNameValuePair("To", clientNumber))
      params ::= (new BasicNameValuePair("From", global.config.twilioNumber))
      params ::= (new BasicNameValuePair("Body", message))

      // Creates and sends the SMS
      val twilioClient = new TwilioRestClient(
        global.config.twilioAccountSid,
        global.config.twilioAuthToken)
      val account = twilioClient.getAccount()
      val factory = account.getMessageFactory()
      factory.create(params)
    } catch {
      case e: TwilioRestException =>
        println(s"ERROR: Could not send the SMS.\n${e.getErrorMessage()}")
    }
  }
}

object SmsVerificationSender {
  def apply() = {
    new SmsVerificationSender()
  }
}

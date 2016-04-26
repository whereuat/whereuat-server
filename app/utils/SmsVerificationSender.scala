package utils

import collection.JavaConversions._

import com.twilio.sdk.resource.factory.MessageFactory
import com.twilio.sdk.resource.instance.{Account, Message}
import com.twilio.sdk.{TwilioRestClient, TwilioRestException}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.NameValuePair

// Singleton object for sending verification codes through SMS to clients that
// request an account
object SmsVerificationSender {
  // Method to build and send the SMS message through Twilio
  def send(clientNumber: String, vCode: String): Unit = {
    try {
      // Build the message parameters
      var params = List[NameValuePair]()
      params ::= (new BasicNameValuePair("To", clientNumber))
      params ::= (new BasicNameValuePair("From", global.config.twilioNumber))
      val message = s"Your whereu@ verification code is $vCode. Input this " +
                    s"code into the whereu@ app to create your account."
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

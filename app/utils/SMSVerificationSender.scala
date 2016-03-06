package controllers

import collection.JavaConversions._

import com.twilio.sdk.resource.factory.MessageFactory
import com.twilio.sdk.resource.instance.Account
import com.twilio.sdk.resource.instance.Message
import com.twilio.sdk.TwilioRestClient
import com.twilio.sdk.TwilioRestException
import org.apache.http.message.BasicNameValuePair
import org.apache.http.NameValuePair


class SMSVerificationSender {
  private val AccountSid: String = global.config.twilioAccountSid
  private val AuthToken: String = global.config.twilioAuthToken
  private val TwilioNumber: String = global.config.twilioNumber

  def send(clientNumber: String, message: String) {
    try {
      val twilioClient: TwilioRestClient = new TwilioRestClient(
        AccountSid, AuthToken)
      val account: Account = twilioClient.getAccount()

      val factory: MessageFactory = account.getMessageFactory()
      var params = List[NameValuePair]()

      params ::= (new BasicNameValuePair("To", clientNumber));
      params ::= (new BasicNameValuePair("From", TwilioNumber));
      params ::= (new BasicNameValuePair("Body", message));
      // Creates and sends the SMS
      factory.create(params);
    } catch {
      case e: TwilioRestException =>
        println(s"ERROR: Could not send the SMS.\n${e.getErrorMessage()}")
    }
  }
}

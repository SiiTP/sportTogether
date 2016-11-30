package messages
import spray.json.{JsString, JsNumber, JsObject}
/**
  * Created by ivan on 14.11.16.
  */
trait FcmMessage {
  def messageType: Int
  def title: String
  def toJsonObject: JsObject
}
object FcmMessage {
  val CANCELLED = 0
  val RESULT = 1
  val REMIND = 2
  val USER_LEFT = 3
}
case class FcmTextMessage(mesage: String, _title: String,_messageType: Int) extends FcmMessage {
  override def title = _title
  override def messageType = _messageType

  override def toJsonObject: JsObject = {
    new JsObject(
      Map(
        "type" -> JsNumber(messageType),
        "message" -> JsString(mesage),
        "title" -> JsString(title)
      ))
  }
}
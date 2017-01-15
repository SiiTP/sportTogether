package messages
import spray.json._
import entities.db.EntitiesJsonProtocol._;
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
case class FcmTextMessage(mesage: String, _title: String,_messageType: Int, obj: Option[JsValue] = None) extends FcmMessage {
  override def title = _title
  override def messageType = _messageType
  def jsonMapValues: Map[String, JsValue] = {
    val m = Map(
      "type" -> JsNumber(messageType),
      "message" -> JsString(mesage),
      "title" -> JsString(title)
    )
    obj match {
      case Some(value) =>
        m.updated("object",value)
      case None =>
        m
    }
  }
  override def toJsonObject: JsObject = {
    new JsObject(
      jsonMapValues
      )
  }
}
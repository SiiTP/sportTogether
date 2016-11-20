package service

import akka.util.Timeout
import dao.{UserDAO, EventsDAO}

import scala.None
import scala.util.{Failure, Success}
import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import entities.db.UserJoinEvent
import messages.FcmTextMessage
import service.MessageService.{SendTokensTextMessage, InitJoinEventService, SendEventsTextMessageToCreator, SendEventsTextMessage}
import spray.json.{JsString, JsNumber, JsObject}
import scala.concurrent.duration._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global

class MessageService {

}
object MessageService {
  case class SendEventsTextMessage(eventId: Int, textMessage: FcmTextMessage)
  case class SendTokensTextMessage(tokens: Seq[String], textMessage: FcmTextMessage)
  case class SendEventsTextMessageToCreator(eventId: Int, textMessage: FcmTextMessage)

  case class InitJoinEventService(jesActor: AskableActorRef)
}
class MessageServiceActor(fcmServiceActor: ActorRef) extends Actor {
  implicit lazy val timeouts = Timeout(10.seconds)
  val logger = Logger("webApp")
  private var _joinEventServiceActor: Option[AskableActorRef] = None
  def joinEventServiceActor(joinEventServiceActor: AskableActorRef) = _joinEventServiceActor = Some(joinEventServiceActor)
  override def receive = {
    case SendEventsTextMessage(eventId, msg) =>
      _joinEventServiceActor match {
        case Some(actor) =>
          (actor ? JoinEventService.GetTokens(eventId)).onComplete {
            case Success(tokens) =>
              logger.debug("tokens to send : " + tokens)
              fcmServiceActor ! FcmService.SendMessage(tokens.asInstanceOf[Seq[UserJoinEvent]].map(_.deviceToken).seq, msg.toJsonObject)
            case Failure(t) =>
              logger.debug("exception", t)
          }
        case None =>
      }
    case SendTokensTextMessage(tokens, msg) =>
      fcmServiceActor ! FcmService.SendMessage(tokens, msg.toJsonObject)
    case SendEventsTextMessageToCreator(eventId, msg) =>
      _joinEventServiceActor match {
        case Some(actor) =>
          (actor ? JoinEventService.GetCreatorToken(eventId)).onComplete {
            case Success(token) =>
              val tokenString = token.asInstanceOf[String]
              logger.debug(s"creator token got $tokenString")
              fcmServiceActor ! FcmService.SendMessage(Seq(tokenString), msg.toJsonObject)
            case Failure(t) =>
              logger.debug("exception", t)
          }
        case None =>
      }
    case InitJoinEventService(actor) =>
      joinEventServiceActor(actor)
  }
}
package service

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import entities.db.UserJoinEvent
import messages.FcmTextMessage
import service.MessageService.{InitJoinEventService, SendEventsTextMessage, SendEventsTextMessageToCreator, SendTokensTextMessage}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

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
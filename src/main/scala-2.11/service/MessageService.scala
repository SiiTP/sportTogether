package service

import akka.util.Timeout

import scala.util.{Failure, Success}
import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import entities.db.UserJoinEvent
import messages.FcmTextMessage
import service.MessageService.SendEventsTextMessage
import spray.json.{JsString, JsNumber, JsObject}
import scala.concurrent.duration._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 14.11.16.
  */
class MessageService {

}
object MessageService {
  case class SendEventsTextMessage(eventId: Int, textMessage: FcmTextMessage)
}
class MessageServiceActor(joinEventService: AskableActorRef, fcmServiceActor: ActorRef) extends Actor {
  implicit lazy val timeouts = Timeout(10.seconds)
  val logger = Logger("webApp")
  override def receive = {
    case SendEventsTextMessage(eventId, msg) =>
      (joinEventService ? JoinEventService.GetTokens(eventId)).onComplete {
        case Success(tokens) =>
          logger.debug("tokens to send : " + tokens)
          fcmServiceActor ! FcmService.SendMessage(tokens.asInstanceOf[Seq[UserJoinEvent]].map(_.deviceToken).seq, msg.toJsonObject)
//          fcmServiceActor ! FcmService.SendMessage(Seq("eip4vuQhWQU:APA91bEFEEZKOAUBoKwa3RsjU7oTcKTVbWdZbqZ5JB4d5vjJH7H8kFN3hKWKuOovhShpLVt6asIsiWVZdLZvsDHAraftWgltTNMixG7TmQwphH-vjQ6TVMC-QxZs6FZBM8tCJ7O2Qa8v"), msg.toJsonObject)
        case Failure(t) =>
          logger.info("exception " + t.getMessage)
      }
  }
}
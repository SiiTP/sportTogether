package service

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import com.typesafe.scalalogging.Logger
import dao.{EventsDAO, EventUsersDAO}
import entities.db.{UserJoinEvent, User, MapEvent}
import response.{JoinServiceResponse, EventResponse}
import service.InMemoryEventService.AddUserToEvent
import entities.db.EntitiesJsonProtocol._
import spray.json.{JsNumber, JsString, JsObject}
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 12.10.16.
  */
class InMemoryEventService {
  private val logger = Logger("webApp")
  private val events = new mutable.HashMap[MapEvent,mutable.Seq[String]]
  private val users = new mutable.HashMap[User,String]
  private val eventUsersDAO = new EventUsersDAO()
  private val eventsDAO = new EventsDAO()

  //if store in database replace with dao
  def add(userJoinEvent: UserJoinEvent) = {
    logger.debug(s"add ${userJoinEvent.userId} to event ${userJoinEvent.eventId} , token:${userJoinEvent.deviceToken}")
    eventUsersDAO.create(userJoinEvent)
  }

  def isExist(event: MapEvent) = events.contains(event)
  def isUserAlreadyJoined(user: User, eid: Int) = {
    eventUsersDAO.isAlreadyJoined(UserJoinEvent(user.id.get, null, eid))
  }
  def leaveEvent(user: User, event: MapEvent) = {
    users.get(user) match {
      case Some(token) =>
        events.get(event) match {
          case Some(res) =>
            logger.debug(s"""leaving event $event by user $user """)
            val newArr = res.filter(!_.equals(token))
            logger.debug("filtered event users " + newArr)
            events.put(event, newArr)
            users.remove(user)
          case None =>
        }
      case None =>
    }
  }
  def getTokens(eId: Int) = {
    eventUsersDAO.getById(eId)
  }
  def update(event: MapEvent) = {
    logger.debug("updating event: " + event)
    events.get(event) match {
      case Some(result) =>
        logger.debug("found event users " + result)
        events.put(event,result)
      case None =>
    }
  }
  def isFullEvent(eventId: Int) = {
    eventsDAO.get(eventId).zip(eventUsersDAO.getById(eventId)).map(tuple => {
      logger.debug(s"eventId: $eventId size ${tuple._1.maxPeople} , events current peoples ${tuple._2.size}")
       tuple._2.size >= tuple._1.maxPeople
    })
  }
}

object InMemoryEventService {
  case class AddUserToEvent(eventId: Int, user: User, token: String)
}

class SenderHelper(sender: ActorRef) {
  var isSendedAnswer = false
  def answer(message: String) = {
    if(!isSendedAnswer){
      isSendedAnswer = true
      sender ! message
    }
  }
}
class InMemoryEventServiceActor(_fcmService: ActorRef) extends Actor {
  private val logger = Logger("webApp")
  private val service = new InMemoryEventService()
  override def receive: Receive = {
    case AddUserToEvent(ev,user, token) =>
      val sended = new SenderHelper(sender())
      val userJoinEvent = UserJoinEvent(user.id.get, token, ev)
      logger.debug("trying to add: " + userJoinEvent)
      service.isUserAlreadyJoined(user, ev)
        .flatMap(isFullChain(_)(userJoinEvent,sended))
        .flatMap(addChain(_)(userJoinEvent,sended)).onComplete {
        case Success(res) => res match {
          case item: Some[UserJoinEvent] =>
            sendNotifyToEventUsers(ev,user)
            sended.answer(JoinServiceResponse.responseSuccess(item).toJson.prettyPrint)
          case None =>
        }
        case Failure(t) =>
          sended.answer(JoinServiceResponse.unexpectedError.toJson.prettyPrint)


      }

  }
  private def sendNotifyToEventUsers(eId: Int, user: User) = {
    service.getTokens(eId).onSuccess {
      case tokens =>
        val obj = new JsObject(Map(("name" -> JsNumber(user.id.get)),("clientId" -> JsString(user.clientId)) ))

        _fcmService ! FcmService.SendMessage(tokens.map(_.deviceToken).seq,obj)
    }
  }
  private def isFullChain(result: Boolean)(userJoinEvent: UserJoinEvent, sender: SenderHelper): Future[Boolean] = {
    logger.debug(s"is joined user? - $result")
    if (!result) {
      service.isFullEvent(userJoinEvent.eventId)
    } else {
      sender.answer(JoinServiceResponse.userAlreadyJoined.toJson.prettyPrint)
      Future{false}
    }
  }
  private def addChain(result: Boolean)(userJoinEvent: UserJoinEvent, sended: SenderHelper): Future[Option[UserJoinEvent]] = {
    logger.debug(s"is full event ? - $result")
    if (!result) {
      service.add(userJoinEvent).map(Some(_))
    } else {
      sended.answer(JoinServiceResponse.eventIsFull.toJson.prettyPrint)
      Future{None}
    }
  }
}

/*
map(isFull => {
              if (!isFull) {
                service.add(ev, user, token)
              } else {
                sended ! JoinServiceResponse.eventIsFull.toJson.prettyPrint
              }
            }).onComplete {
              case Success(userJoinEvent) =>
                sended ! JoinServiceResponse.responseSuccess(Some(userJoinEvent)).toJson.prettyPrint
              case Failure(t) =>
                sended ! JoinServiceResponse.unexpectedError.toJson.prettyPrint
            }
          }
      }
 */
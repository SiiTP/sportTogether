package service

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.Logger
import dao.{EventUsersDAO, EventsDAO}
import entities.db.{MapEvent, MapEventAdapter, User, UserJoinEvent}
import response.JoinServiceResponse
import service.JoinEventService.{AddUserToEvent, GetEventsOfUserJoined, GetTokens}
import entities.db.EntitiesJsonProtocol._
import spray.json.{JsNumber, JsObject, JsString}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 12.10.16.
  */
class JoinEventService {
  private val logger = Logger("webApp")
  private val eventUsersDAO = new EventUsersDAO()
  private val eventsDAO = new EventsDAO()

  //if store in database replace with dao
  def add(userJoinEvent: UserJoinEvent) = {
    logger.debug(s"add ${userJoinEvent.userId} to event ${userJoinEvent.eventId} , token:${userJoinEvent.deviceToken}")
    eventUsersDAO.create(userJoinEvent)
  }

  def getEventsOfUserJoined(user: User): Future[Seq[MapEventAdapter]] = {
    EventService.toAdapterForm(eventUsersDAO.getEventsOfUserJoined(user))
  }

  def isExist(event: MapEvent) = ???
  def isUserAlreadyJoined(user: User, eid: Int) = {
    eventUsersDAO.isAlreadyJoined(UserJoinEvent(user.id.get, null, eid))
  }
  def leaveEvent(user: User, event: MapEvent) = ???
  def getTokens(eId: Int): Future[Seq[UserJoinEvent]] = {
    eventUsersDAO.getById(eId)
  }
  def update(event: MapEvent) = {
    logger.debug("updating event: " + event)
    //todo
  }

  def isFullEvent(eventId: Int) = {
    eventsDAO.get(eventId).zip(eventUsersDAO.getById(eventId)).map(tuple => {
      logger.debug(s"eventId: $eventId size ${tuple._1.maxPeople} , events current peoples ${tuple._2.size}")
       tuple._2.size >= tuple._1.maxPeople
    })
  }
}

object JoinEventService {
  case class AddUserToEvent(eventId: Int, user: User, token: String)
  case class GetEventsOfUserJoined(user: User)
  case class GetTokens(eId: Int)

}

/**
  * тут кароче небольшая обертка над sender(), используется в map ниже
 *
  * @param sender
  */
class SenderHelper(sender: ActorRef) {
  var isSendedAnswer = false
  def answer(message: String) = {
    if(!isSendedAnswer){
      isSendedAnswer = true
      sender ! message
    }
  }
}
class JoinEventServiceActor(service: JoinEventService) extends Actor {
  private val logger = Logger("webApp")
  override def receive: Receive = {
    case AddUserToEvent(ev, user, token) =>
      val sended = new SenderHelper(sender())
      val userJoinEvent = UserJoinEvent(user.id.get, token, ev)
      logger.debug("trying to add: " + userJoinEvent)
      service.isUserAlreadyJoined(user, ev)
        .flatMap(isFullChain(_)(userJoinEvent,sended))
        .flatMap(addChain(_)(userJoinEvent,sended)).onComplete {
        case Success(res) => res match {
          case item: Some[UserJoinEvent] =>
            sended.answer(JoinServiceResponse.responseSuccess(item).toJson.prettyPrint)
          case None =>
        }
        case Failure(t) =>
          t.printStackTrace()
          sended.answer(JoinServiceResponse.unexpectedError.toJson.prettyPrint)
      }

    case GetEventsOfUserJoined(user) =>
      val sended = sender()
      service.getEventsOfUserJoined(user).onComplete {
        case Success(result) =>
          sended ! JoinServiceResponse.responseSuccess[Seq[MapEventAdapter]](Some(result)).toJson.prettyPrint
        case Failure(e) =>
          e.printStackTrace()
          sended ! JoinServiceResponse.unexpectedError.toJson.prettyPrint
      }

    case GetTokens(eId) =>
      val sended = sender()
      service.getTokens(eId).onComplete {
        case Success(result) => sended ! result
        case Failure(e) => e.printStackTrace()
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
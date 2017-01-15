package service

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import dao.{UserDAO, TaskDao, EventUsersDAO, EventsDAO}
import entities.db.{MapEvent, MapEventAdapter, User, UserJoinEvent}
import messages.{FcmMessage, FcmTextMessage}
import response.JoinServiceResponse
import service.JoinEventService._
import entities.db.EntitiesJsonProtocol._
import spray.json._
import entities.db.EntitiesJsonProtocol._
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
  private val tasksDao = new TaskDao()
  //if store in database replace with dao
  def add(userJoinEvent: UserJoinEvent) = {
    logger.debug(s"add ${userJoinEvent.userId} to event ${userJoinEvent.eventId} , token:${userJoinEvent.deviceToken}")
    eventUsersDAO.create(userJoinEvent).andThen {
      case e =>
        e match {
          case Success(uje) =>
            eventsDAO.incUsersNow(userJoinEvent.eventId)
          case Failure(t) =>
        }

    }
  }

  def getEventsOfUserJoined(user: User): Future[Seq[MapEventAdapter]] = {
    EventService.toAdapterForm(eventUsersDAO.getEventsOfUserJoined(user), user)
  }
  def getFutureEvents = eventUsersDAO.getEvents()
  def updateNotified(items: Seq[(Int,Int)]) = eventUsersDAO.updateNotified(items)
  def getJoinedUserToken(userId: Int, eId: Int): Future[String] = {
    eventUsersDAO.getById(eId).map(seq => seq.
      filter(userJoinEvent => userJoinEvent.userId == userId).
      map(_.deviceToken).head)//todo создателя может не быть
  }
  def isExist(event: MapEvent) = ???
  def isUserAlreadyJoined(user: User, eid: Int) = {
    eventUsersDAO.isAlreadyJoined(UserJoinEvent(user.id.get, null, eid))
  }
  def getCreatorToken(eid: Int) = {
    eventsDAO.get(eid).flatMap(event => getJoinedUserToken(event.userId.get, eid))
  }
  def leaveEvent(user: User, eId: Int) = {
    eventUsersDAO.deleteFromEvent(eId, user.id.get).andThen{
      case e =>
        e match {
          case Success(count) =>
            if (count > 0) {
              eventsDAO.decUsersNow(eId)
              tasksDao.resetUserTasksInEvent(user.id.get, eId)
            }
          case Failure(t) =>
        }
    }
  }
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
  case class LeaveEvent(eId: Int, user: User)
  case class GetCreatorToken(eId: Int)
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
class JoinEventServiceActor(service: JoinEventService, messageServiceActor: ActorRef) extends Actor {
  private val logger = Logger("webApp")
  private val eventService = new EventService()
  override def receive: Receive = {
    case AddUserToEvent(ev, user, token) =>
      val sended = new SenderHelper(sender())
      val userJoinEvent = UserJoinEvent(user.id.get, token, ev)
//      logger.debug("trying to add: " + userJoinEvent)
      service.isUserAlreadyJoined(user, ev)
        .flatMap(isFullChain(_)(userJoinEvent,sended))
        .flatMap(addChain(_)(userJoinEvent,sended)).onComplete {
        case Success(res) => res match {
          case item: Some[UserJoinEvent] =>
            sended.answer(JoinServiceResponse.responseSuccess(item).toJson.prettyPrint)
          case None =>
        }
        case Failure(t) =>
          logger.debug("exception add to event ", t)
          sended.answer(JoinServiceResponse.unexpectedError(t.getMessage).toJson.prettyPrint)
      }
    case GetCreatorToken(eId) =>
      val sended = sender()
      service.getCreatorToken(eId).onComplete {
        case Success(token) =>
          sended ! token
        case Failure(e) =>
          sended ! JoinServiceResponse.unexpectedError.toJson.prettyPrint
          logger.debug("exception get creator token :", e)
      }
    case GetEventsOfUserJoined(user) =>
      val sended = sender()
      service.getEventsOfUserJoined(user).onComplete {
        case Success(result) =>
          sended ! JoinServiceResponse.responseSuccess[Seq[MapEventAdapter]](Some(result)).toJson.prettyPrint
        case Failure(e) =>
          logger.debug("exception :", e)
          sended ! JoinServiceResponse.unexpectedError(e.getMessage).toJson.prettyPrint
      }

    case GetTokens(eId) =>
      val sended = sender()
      service.getTokens(eId).onComplete {
        case Success(result) => sended ! result
        case Failure(e) =>
          logger.debug("exception get tokens: ", e)
          sended ! JoinServiceResponse.unexpectedError(e.getMessage).toJson.prettyPrint
      }
    case LeaveEvent(eId, user) =>
      val sended = sender()
      service.leaveEvent(user,eId).onComplete {
        case Success(result) =>
          if (result == 0) {
            sended ! JoinServiceResponse.userNotFoundInEvent.toJson.prettyPrint
          } else {
            sended ! JoinServiceResponse.responseSuccess(Some(s"Событий покинуто: $result")).toJson.prettyPrint
            eventService.getEvent(eId).andThen{
              case optEvent =>
                optEvent match {
                  case Success(event) =>
                    messageServiceActor ! MessageService.SendEventsTextMessageToCreator(eId,
                      FcmTextMessage(s"Пользователь ${user.name.getOrElse("")} покинул событие",s"Пользователь покинул событие ${event.name}",FcmMessage.USER_LEFT, Some(event.toJson)))
                  case Failure(e) =>
                    logger.debug("exception leave event:", e)
                    sended ! JoinServiceResponse.unexpectedError(e.getMessage).toJson.prettyPrint
                }

            }

          }
        case Failure(e) => {
          logger.debug("exception leave event:", e)
          sended ! JoinServiceResponse.unexpectedError(e.getMessage).toJson.prettyPrint
        }
      }
  }
  private def isFullChain(result: Boolean)(userJoinEvent: UserJoinEvent, sender: SenderHelper): Future[Boolean] = {
    if (!result) {
      service.isFullEvent(userJoinEvent.eventId)
    } else {
      sender.answer(JoinServiceResponse.userAlreadyJoined.toJson.prettyPrint)
      Future.successful{false}
    }
  }
  private def addChain(result: Boolean)(userJoinEvent: UserJoinEvent, sended: SenderHelper): Future[Option[UserJoinEvent]] = {
    if (!result) {
      service.add(userJoinEvent).map(Some(_))
    } else {
      sended.answer(JoinServiceResponse.eventIsFull.toJson.prettyPrint)
      Future.successful{None}
    }
  }
}
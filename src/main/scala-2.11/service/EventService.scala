package service

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import dao.{CategoryDAO, EventUsersDAO, EventsDAO}
import dao.filters.{CategoryFilters, EventFilters}
import entities.db._
import messages.{FcmMessage, FcmTextMessage}
import response.EventResponse
import service.EventService._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import entities.db.EntitiesJsonProtocol._

import scala.concurrent.duration._
/**
  * Created by ivan on 25.09.16.
  */
class EventService {
  val logger = Logger("webApp")
  private val eventsDAO = new EventsDAO()

  def updateEvent(event: MapEvent, user: User): Future[Int] = {
    getUserEvents(user).flatMap { events =>
      events.map(_.id).contains(event.id) match {
        case true => eventsDAO.update(event)
        case false => Future.successful(0)
      }
    }
  }

  def updateResult(event: MapEventResultAdapter, mapEvent: MapEvent): Future[Int] = {
    val updatedEvent = mapEvent.copy(result = event.result, isEnded = true)
    logger.info("updated event : " + updatedEvent)
    eventsDAO.update(updatedEvent)
  }

  def reportEvent(id: Int, user: User) = {
    eventsDAO.reportEvent(id, user)
  }
  def addSimpleEvent(event: MapEvent, user: User) = {
    eventsDAO.create(event.copy(userId = user.id))
  }
  def getUserEvents(user: User) = {
    eventsDAO.eventsByUserId(user.id.getOrElse(0))
  }
  def getEventsAround(filters: EventFilters) = {
    eventsDAO.getEvents(filters)
  }
  def finishEvent(id: Int, user: User) = {
    eventsDAO.endEvent(id, user.id.getOrElse(0))
  }
  def getUserEvents(id: Int) = eventsDAO.eventsByUserId(id)
  def getEvent(id: Int) = eventsDAO.get(id)
  def getEventsInDistance(distance: Double, lon: Double, lat: Double, filters: EventFilters) = eventsDAO.getNearestEventsByDistance(distance,lon,lat, filters)
}

object EventService {
  case class AddEvent(event: MapEventAdapter,user: User)
  case class GetEvents(filters: EventFilters)
  case class GetUserEvents(id: Int)
  case class GetEvent(id: Int)
  case class GetEventsByDistance(distance: Double, longtitude: Double, latitude: Double, filters: EventFilters)
  case class UpdateEvent(event: MapEvent, user: User)
  case class ReportEvent(id: Int, user: User)
  case class GetEventsByCategoryId(id: Int)
  case class FinishEvent(id: Int, user: User)
  case class UpdateEventResult(event: MapEventResultAdapter, user: User)

  val logger = Logger("webApp")

  private val eventsDAO = new EventsDAO()
  private val eventUsersDAO = new EventUsersDAO()
  private val categoryDAO = new CategoryDAO()

  def toAdapterForm(futureSeq: Future[Seq[MapEvent]]): Future[Seq[MapEventAdapter]] = {
    logger.info("in to adapter form")
    val futureAdapters = futureSeq.flatMap(seq => {
      Future.sequence(
        seq.map(mapEvent => {
          val isJoinedFuture = eventUsersDAO.isUserJoined(mapEvent.userId, mapEvent.id)
          val isReportedFuture = eventsDAO.isEventReported(mapEvent.userId, mapEvent.id)
          val categoryFuture = categoryDAO.get(mapEvent.categoryId)
          val countUsersFuture = eventsDAO.getCountUsersInEvent(mapEvent.id)
          for {
            isJoined <- isJoinedFuture
            isReported <- isReportedFuture
            category <- categoryFuture
            countUsers <- countUsersFuture
          } yield MapEventAdapter(
            mapEvent.name,
            category,
            mapEvent.latitude,
            mapEvent.longtitude,
            mapEvent.date,
            Some(countUsers),
            mapEvent.maxPeople,
            mapEvent.reports,
            mapEvent.description,
            mapEvent.result,
            mapEvent.isEnded,
            isJoined,
            isReported,
            mapEvent.userId,
            mapEvent.id
          )
        })
      )
    })
    futureAdapters.recoverWith {
      case e: Throwable =>
        logger.error("in to adapter function : " + e.getMessage)
        Future.successful(Seq.empty[MapEventAdapter])
    }
  }
}
class EventServiceActor(eventService: EventService,
                        remingderServiceActor: ActorRef,
                        categoyService: CategoryService,
                        messageServiceActor: ActorRef,
                        joinEventService: JoinEventService) extends Actor {
  implicit lazy val timeouts = Timeout(4.seconds)


  override def receive = {
    case AddEvent(event,user) =>
      val sended = sender()
      categoyService.getAllCategories(new CategoryFilters(Map("category:name"-> List(event.category.name)))).onSuccess {
        case categories =>
          logger.debug("found categories match names " + categories)
          var response: Future[MapEvent] = null
          if (categories.nonEmpty) {
            response = eventService.addSimpleEvent(event.copy(category = categories.head).toMapEvent,user)
          } else {
            response = categoyService.createCategory(event.category.name).flatMap((category: MapCategory) => {
              logger.debug("created new category: " + category)
              eventService.addSimpleEvent(event.copy(category = category).toMapEvent,user)
            })
          }

          new EventsServiceFetcher().fetchOne(response).onComplete {
            case Success(result) =>
              sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
              remingderServiceActor ! ReminderService.Add(result.toMapEvent)
            case Failure(t) =>
              t.printStackTrace()
              sended ! EventResponse.unexpectedError.toJson.prettyPrint
          }
      }
    case GetEvents(filters) =>
      val sended = sender()
      new EventsServiceFetcher().fetch(eventService.getEventsAround(filters)).onComplete {
        case Success(result) =>
          logger.info(s"success when get events : " + result)
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(e) =>
          logger.info(s"fail when get events : " + e.getMessage)
          sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
      }
    case GetUserEvents(id) =>
      val sended = sender()
      new EventsServiceFetcher().fetch(eventService.getUserEvents(id)).onSuccess {
        case eventsSeq => sended ! EventResponse.responseSuccess(Some(eventsSeq)).toJson.prettyPrint
      }
    case GetEvent(id) =>
      val sended = sender()
      val eventFuture = eventService.getEvent(id)
      new EventsServiceFetcher().fetchOne(eventFuture).onComplete {
        case Success(event) => sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.notFoundError.toJson.prettyPrint
      }
    case GetEventsByDistance(distance, longtitude, latitude, filters) =>
      val sended = sender()
      new EventsServiceFetcher().fetch(eventService.getEventsInDistance(distance, longtitude, latitude, filters)).onComplete {
        case Success(events) => sended ! EventResponse.responseSuccess(Some(events)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }

    case UpdateEvent(event, user) =>
      logger.debug("UPDATING " + event + " \nUSER " + user)

      val sended = sender()
      eventService.updateEvent(event,user).onComplete {
        case Success(updatedEvent) if updatedEvent > 0 =>
          sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Success(updatedEvent) if updatedEvent == 0 =>
          sended ! EventResponse.unexpectedError.toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }

    case UpdateEventResult(event, user) =>
      logger.debug("updating result " + event + " \nUSER " + user)

      val sended = sender()
      eventService.getEvent(event.id).onComplete {
        case Success(mapEvent) =>
          if (mapEvent.userId == user.id && !mapEvent.isEnded) {
            eventService.updateResult(event, mapEvent).onComplete {
              case Success(updatedEvent) if updatedEvent > 0 =>
                sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
                messageServiceActor ! MessageService.SendEventsTextMessage(event.id,
                  FcmTextMessage("Итог события", event.result.getOrElse(""), FcmMessage.RESULT))
              case Success(updatedEvent) if updatedEvent == 0 =>
                sended ! EventResponse.notFoundError.toJson.prettyPrint
              case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
            }
          } else {
            sended ! EventResponse.alreadyPostedResult.toJson.prettyPrint
          }
        case Failure(t) =>
          logger.debug("exception get event", t)
          sended ! EventResponse.notFoundError.toJson.prettyPrint
      }

    case ReportEvent(id, user) =>
      val sended = sender()
      eventService.reportEvent(id, user).onComplete {
        case Success(result) => sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
        case Failure(t) =>
          logger.debug("exception ", t)
          sended ! EventResponse.alreadyReport.toJson.prettyPrint
      }
    case FinishEvent(id, user) =>
      val sended = sender()
      joinEventService.getTokens(id).onComplete {
        case Success(tokens) =>
          eventService.finishEvent(id, user).onComplete {
            case Success(result) =>
              sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
              messageServiceActor ! MessageService.SendTokensTextMessage(tokens.map(_.deviceToken),FcmTextMessage("Событие отменено","Отмена события", FcmMessage.CANCELLED))
            case Failure(t) =>
              logger.info("join event failure : ", t)
              sended ! EventResponse.alreadyReport.toJson.prettyPrint
          }
        case Failure(t) =>
          logger.debug("exception ", t)
          sended ! EventResponse.unexpectedError(t.getMessage).toJson.prettyPrint
      }

  }

}

class EventsServiceFetcher() {
  val categoryDAO = new CategoryDAO()
  val eventsDAO = new EventsDAO()
  val logger = Logger("webApp")
  def fetch(eventsFuture: Future[Seq[MapEvent]]): Future[Seq[MapEventAdapter]] = {
    EventService.toAdapterForm(eventsFuture)
  }
  def fetchOne(f: Future[MapEvent]): Future[MapEventAdapter] = {
    f.flatMap((f:MapEvent) => {
      fetchCategory(Seq(f)).map((f:Seq[MapEventAdapter]) => f.head)
    })
  }
  private def fetchCategory(events: Seq[MapEvent]): Future[Seq[MapEventAdapter]] = {
    val catIds = events.map(_.categoryId).toSet
    val params = Map("category:id" -> catIds.map(_.toString).toList)
    categoryDAO.getCategories(new CategoryFilters(params)).map((categories:Seq[MapCategory]) => {
      events.map((mapEvent: MapEvent) => {
        MapEventAdapter(
          mapEvent.name,
          categories.find(_.id.get == mapEvent.categoryId).getOrElse(MapCategory("",None)),
          mapEvent.latitude,
          mapEvent.longtitude,
          mapEvent.date,
          Some(0),
          mapEvent.maxPeople,
          mapEvent.reports,
          mapEvent.description,
          mapEvent.result,
          mapEvent.isEnded,
          false,
          false,
          mapEvent.userId,
          mapEvent.id
        )
      })
    })
  }
}
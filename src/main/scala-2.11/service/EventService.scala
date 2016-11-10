package service

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import dao.{CategoryDAO, EventsDAO}
import dao.filters.{CategoryFilters, EventFilters, ParametersFiltration}
import entities.db._
import response.{CategoryResponse, EventResponse}
import service.EventService._

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import response.MyResponse._
import entities.db.EntitiesJsonProtocol._

import scala.concurrent.duration._
/**
  * Created by ivan on 25.09.16.
  */
class EventService {
  private val eventsDAO = new EventsDAO()

  def updateEvent(event: MapEvent, user: User): Future[Int] = {
    getUserEvents(user).flatMap { events =>
      events.map(_.id).contains(event.id) match {
        case true => eventsDAO.update(event)
        case false => Future.successful(0)
      }
    }
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

}
class EventServiceActor(eventService: EventService, remingderServiceActor: ActorRef, categoyService: CategoryService) extends Actor {
  val logger = Logger("webApp")


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
          new EventsServiceFetcher(null).fetchOne(response).onComplete {
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
      new EventsServiceFetcher(eventService.getEventsAround(filters)).fetch().onComplete {
        case Success(result) =>
          logger.info(s"success when get events : " + result)
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(e) =>
          logger.info(s"fail when get events : " + e.getMessage)
          sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
      }
    case GetUserEvents(id) =>
      val sended = sender()
      new EventsServiceFetcher(eventService.getUserEvents(id)).fetch().onSuccess {
        case eventsSeq => sended ! EventResponse.responseSuccess(Some(eventsSeq)).toJson.prettyPrint
      }
    case GetEvent(id) =>
      val sended = sender()
      val eventFuture = eventService.getEvent(id).map(Seq(_))
      new EventsServiceFetcher(eventFuture).fetch().onComplete {
        case Success(event) => sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.notFoundError.toJson.prettyPrint
      }
    case GetEventsByDistance(distance, longtitude, latitude, filters) =>
      val sended = sender()
      new EventsServiceFetcher(eventService.getEventsInDistance(distance, longtitude, latitude, filters)).fetch().onComplete {
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
    case ReportEvent(id, user) =>
      val sended = sender()
      eventService.reportEvent(id, user).onComplete {
        case Success(result) => sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.alreadyReport.toJson.prettyPrint
      }
    case FinishEvent(id, user) =>
      val sended = sender()
      eventService.finishEvent(id, user).onComplete {
        case Success(result) => sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.alreadyReport.toJson.prettyPrint
      }
  }
}

class EventsServiceFetcher(eventsFuture: Future[Seq[MapEvent]]) {
  val categoryDAO = new CategoryDAO()
  val eventsDAO = new EventsDAO()
  def fetch(): Future[Seq[MapEventAdapter]] = {
    eventsFuture.flatMap((f:Seq[MapEvent]) => {
      fetchCategory(f)
    })
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
          eventsDAO.getCountUsersInEvent(mapEvent.id.getOrElse(0)),
          mapEvent.maxPeople,
          mapEvent.reports,
          mapEvent.description,
          mapEvent.isEnded,
          mapEvent.userId,
          mapEvent.id
        )
      })
    })
  }

}
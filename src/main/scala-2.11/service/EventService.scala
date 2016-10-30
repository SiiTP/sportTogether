package service

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import com.typesafe.scalalogging.Logger
import dao.EventsDAO
import dao.filters.{EventFilters, ParametersFiltration}
import entities.db.{MapEvent, User, UserReport}
import response.{CategoryResponse, EventResponse}
import service.EventService._

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
  def joinEvent(id: Int, user: User, deviceToken: String) = {

  }
  def getUserEvents(user: User) = {
    eventsDAO.eventsByUserId(user.id.getOrElse(0))
  }
  def getEventsAround(filters: EventFilters) = {
    eventsDAO.getEvents(filters)
  }
  def getCategoryEvents(id: Int) = eventsDAO.eventsByCategoryId(id)
  def getUserEvents(id: Int) = eventsDAO.eventsByUserId(id)
  def getEvent(id: Int) = eventsDAO.get(id)
  def getEventsInDistance(distance: Double, lon: Double, lat: Double, filters: EventFilters) = eventsDAO.getNearestEventsByDistance(distance,lon,lat, filters)
}

object EventService {
  case class AddEvent(event:MapEvent,user:User)

  case class GetEvents(filters: EventFilters)
  case class GetUserEvents(id: Int)
  case class GetEvent(id: Int)
  case class GetEventsByDistance(distance: Double, longtitude: Double, latitude: Double, filters: EventFilters)

  case class UpdateEvent(event: MapEvent, user: User)
  case class ReportEvent(id: Int, user: User)
  case class GetEventsByCategoryId(id: Int)
}
class EventServiceActor(eventService: EventService, remingderServiceActor: ActorRef) extends Actor {
  val logger = Logger("webApp")

  override def receive = {
    case AddEvent(event,user) =>
      val response = eventService.addSimpleEvent(event,user)
      val sended = sender()
      response.onComplete {
        case Success(result) =>
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
          remingderServiceActor ! ReminderService.Add(result)
        case Failure(t) =>
          t.printStackTrace()
          sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }
    case GetEvents(filters) =>
      val sended = sender()
      eventService.getEventsAround(filters).onComplete {
        case Success(result) =>
          logger.info(s"success when get events : " + result)
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(e) =>
          logger.info(s"fail when get events : " + e.getMessage)
          sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
      }
    case GetUserEvents(id) =>
      val sended = sender()
      eventService.getUserEvents(id).onSuccess {
        case eventsSeq => sended ! EventResponse.responseSuccess(Some(eventsSeq)).toJson.prettyPrint
      }
    case GetEvent(id) =>
      val sended = sender()
      eventService.getEvent(id).onComplete {
        case Success(event) => sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.notFoundError.toJson.prettyPrint
      }
    case GetEventsByDistance(distance, longtitude, latitude, filters) =>
      val sended = sender()
      eventService.getEventsInDistance(distance, longtitude, latitude, filters).onComplete {
        case Success(events) => sended ! EventResponse.responseSuccess(Some(events)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }
    case UpdateEvent(event, user) =>
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
    case GetEventsByCategoryId(id) =>
      val sended = sender()
      eventService.getCategoryEvents(id).onComplete {
        case Success(result) =>
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.notFoundError.toJson.prettyPrint
      }
  }
}
package service

import akka.actor.Actor
import akka.actor.Actor.Receive
import dao.EventsDAO
import entities.db.{User, MapEvent}
import response.EventResponse
import service.EventService._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by ivan on 25.09.16.
  */
class EventService {
  private val eventsDAO = new EventsDAO()
  def addSimpleEvent(event: MapEvent, user: User) = {
    eventsDAO.create(event.copy(userId = user.id))
  }
  def getUserEvents(user: User) = {
    eventsDAO.eventsByUserId(user.id.getOrElse(0))
  }
  def getEventsAround = {
    eventsDAO.allEvents()
  }
  def getUserEvents(id: Int) = eventsDAO.eventsByUserId(id)
  def getEvent(id: Int) = eventsDAO.get(id)
}

object EventService{
  case class AddEvent(event:MapEvent,user:User)
  case class GetEvents()

  case class GetUserEvents(id: Int)
  case class GetEvent(id: Int)
}
class EventServiceActor(eventService: EventService) extends Actor {
  override def receive = {
    case AddEvent(event,user) =>
      val response = eventService.addSimpleEvent(event,user)
      val sended = sender()
      response.onComplete {
        case Success(result) => sended ! EventResponse.responseSuccess(Some(result))
        case Failure(t) => sended ! EventResponse.unexpectedError
      }
    case GetEvents() =>
      val sended = sender()
      eventService.getEventsAround.onSuccess {
        case result => sended ! result
      }
    case GetUserEvents(id) =>
      val sended = sender()
      eventService.getUserEvents(id).onSuccess {
        case eventsSeq => sended ! eventsSeq
      }
    case GetEvent(id) =>
      val sended = sender()
      eventService.getEvent(id).onComplete {
        case Success(event) => sended ! EventResponse.responseSuccess(Some(event))
        case Failure(t) => sended ! EventResponse.notFoundError
      }
  }
}
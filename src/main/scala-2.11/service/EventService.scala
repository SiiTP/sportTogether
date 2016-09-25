package service

import akka.actor.Actor
import akka.actor.Actor.Receive
import dao.EventsDAO
import entities.db.{User, MapEvent}
import service.EventService.{ResponseEvent, AddEvent}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 25.09.16.
  */
class EventService {
  private val eventsDAO = new EventsDAO()
  def addSimpleEvent(event : MapEvent, user:User) = {
    eventsDAO.create(event.copy(userId = user.id))
  }
  def getUserEvents(user:User) = {
    eventsDAO.eventsByUserId(user.id.getOrElse(0))
  }
}

object EventService{
  case class AddEvent(event:MapEvent,user:User)

  case class ResponseEvent(event: MapEvent)

}
class EventServiceActor(eventService: EventService) extends Actor {
  override def receive = {
    case AddEvent(e,u) => {
      val response = eventService.addSimpleEvent(e,u)
      val sended = sender()
      response.onSuccess{
        case result =>{
          println("result !")
          sended ! ResponseEvent(result)
        }
      }
    }
  }
}
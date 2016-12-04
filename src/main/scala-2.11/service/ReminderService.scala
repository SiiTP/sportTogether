package service

import java.sql.Timestamp
import java.time.{ZoneOffset, LocalDateTime, Period, Instant}
import java.util.concurrent.CopyOnWriteArrayList

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import entities.db.MapEvent
import messages.{FcmMessage, FcmTextMessage}
import service.ReminderService.Add
import spray.json.{JsString, JsObject}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.parallel.immutable
import scala.concurrent.duration.Duration

/**
  * Created by ivan on 17.10.16.
  */
class ReminderService(_fcmService: ActorRef) extends Thread{
  private val logger = Logger("webApp")
  private val joinService = new JoinEventService()
  private var array = new immutable.ParVector[MapEvent]()
  def addEvent(event: MapEvent): Unit = {
    array = array :+ event
//    logger.debug("add event: " + event)
//    logger.debug("size: " + array.length)

  }
  def updateEvent(event: MapEvent) = {
//    logger.debug("updating " + event)
    val index = array.indexWhere(_.id == event.id)
    if (index != -1) {
      array = array.updated(index, event)
    }
  }

  override def run(): Unit = {
    while (!Thread.interrupted()) {
      val workArray = array.filter(event => {
        event.date.toLocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli <= LocalDateTime.now().plusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli
      })
      workArray.map(item => (item.id.get,item.description, item.name))
        .foreach( (i:(Int,Option[String],String)) => {
          joinService.getTokens(i._1).onSuccess {
            case res =>
              _fcmService ! FcmService.SendMessage(
                res.map(_.deviceToken),
                FcmTextMessage(s"Описание:${i._2.getOrElse("")}",s"Остался 1 час до события ${i._3}",FcmMessage.REMIND).toJsonObject
              )
          }
        }
      )
      array = array.filterNot(item => workArray.exists(_.id.eq(item.id)))

      Thread.sleep(1000 * 30) //0.5 minute
    }
  }
}
object ReminderService {
 case class Add(event: MapEvent)
}

class ReminderServiceActor(reminderService: ReminderService) extends Actor {

  override def receive: Receive = {
    case Add(ev) =>
      reminderService.addEvent(ev)
  }
}

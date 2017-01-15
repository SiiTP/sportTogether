package service

import akka.actor.{Actor, ActorRef}
import com.typesafe.scalalogging.Logger
import entities.db.MapEvent
import messages.{FcmMessage, FcmTextMessage}
import service.ReminderService.Add

import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.immutable
import scala.concurrent.ExecutionContext.Implicits.global

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
      joinService.getFutureEvents.onSuccess{
      case (f:Seq[(Int, String,Int,String)]) =>
        val updateArray = new ArrayBuffer[(Int, String,Int,String)]()
        f.groupBy(_._1).values.foreach((values:Seq[(Int, String,Int,String)]) => {
          val name = values.head._4
          _fcmService ! FcmService.SendMessage(
            values.map(_._2),
            FcmTextMessage(s"Скоро начнется событие: $name", "Уведомление о событии", FcmMessage.REMIND).toJsonObject
          )
          values.copyToBuffer(updateArray)
        })
        joinService.updateNotified(updateArray.map((item:(Int, String,Int,String))=> {
          (item._1,item._3)
        }))
      }

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

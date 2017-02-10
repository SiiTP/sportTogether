package service.support

import akka.actor.Actor
import com.typesafe.scalalogging.Logger
import dao.EventsDAO
import service.support.DeleteUnfinishedExpiredEventActor.CheckUnfinishedEvents

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by ivan on 20.01.17.
  */
class DeleteUnfinishedExpiredEventActor extends Actor{
  val eventsDao = new EventsDAO()
  val logged = Logger("special")
  override def receive: Receive = {
    case CheckUnfinishedEvents() =>
      logged.info("CHECK DELETE EVENTS")
      eventsDao.deleteUnfinishedExpiredEvents().onComplete {
        case Success(count) =>
          if (count > 0) {
            logged.info(s"DELETED EVENT SUCCESS, EVENTS COUNT: $count")
          } else {
            logged.info(s"NO EVENT FOR DELETE")
          }
        case Failure(e) =>
          logged.info("exception update events", e)

      }
  }
}

object DeleteUnfinishedExpiredEventActor {
  case class CheckUnfinishedEvents()
}


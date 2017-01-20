package service.support

import akka.actor.Actor
import akka.actor.Actor.Receive
import service.support.ExpiredStatusActor.CheckExpired
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.Logger
import dao.EventsDAO
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 20.01.17.
  */
class ExpiredStatusActor extends Actor{
  val eventsDao = new EventsDAO()
  val logged = Logger("special")
  override def receive: Receive = {
    case CheckExpired() =>
      logged.info("UPDATING EVENT STATUS")
      eventsDao.updateEventsStatus().onComplete {
        case Success(count) =>
          if (count > 0) {
            logged.info(s"UPDATING EVENT SUCCESS, EVENTS COUNT: $count")
          } else {
            logged.info(s"NO EVENT FOR UPDATE")
          }
        case Failure(e) =>
          logged.info("exception update events", e)

      }
  }
}

object ExpiredStatusActor {
  case class CheckExpired()
}

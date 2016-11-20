package dao


import com.typesafe.scalalogging.Logger
import entities.db._
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
/**
  * Created by ivan on 15.10.16.
  */
class EventUsersDAO extends DatabaseDAO[UserJoinEvent, Int]{
  private val table = Tables.eventUsers


  private val tableEvent = Tables.events
  private val logger = Logger("webApp")
  override def create(r: UserJoinEvent): Future[UserJoinEvent] = {
    execute(table += r).map(f => r)
  }

  override def update(r: UserJoinEvent): Future[Int] = {
    execute(table.update(r))
  }

  def getById(eventId: Int): Future[Seq[UserJoinEvent]] = {
    execute(table.filter(_.eventId === eventId).result)
  }

  override def delete(r: UserJoinEvent): Future[Int] = {
    execute(table.filter(_.eventId === r.eventId).delete)
  }

  def deleteFromEvent(eId: Int, userId: Int) = {
    execute(table.filter(uie => uie.eventId === eId && uie.userId === userId).delete)
  }
  def isAlreadyJoined(r: UserJoinEvent) = {
    val query = table.filter(uie => uie.eventId === r.eventId && uie.userId === r.userId).exists
    execute(query.result)
  }

  def isUserJoined(idUser: Option[Int], idEvent: Option[Int]): Future[Boolean] = {
    if (idUser.isEmpty && idEvent.isEmpty) {
      return Future.successful(false)
    }
    val query = table.filter(row => row.userId === idUser && row.eventId === idEvent).exists
    execute(query.result)
  }

  def getEventsOfUserJoined(user: User): Future[Seq[MapEvent]] = {
    val idUser = user.id.getOrElse(0)
    val seq = for {
      (e, rel) <- tableEvent join table on (_.id === _.eventId ) if rel.userId === idUser
    } yield e
    execute(seq.result)
  }

  override def get(r: Int): Future[UserJoinEvent] = ???
}

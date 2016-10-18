package dao

import com.typesafe.scalalogging.Logger
import entities.db._
import slick.dbio
import slick.dbio.DBIOAction
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
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
  def isAlreadyJoined(r: UserJoinEvent) = {
    val query = table.filter(uie => uie.eventId === r.eventId && uie.userId === r.userId).exists
    execute(query.result)
  }

  def getEventsOfUser(user: User): Future[Seq[MapEvent]] = {
    val idUser = user.id.getOrElse(0)
    val seq = for {
      (e, rel) <- tableEvent join table on (_.id === _.eventId ) if rel.userId === idUser
    } yield e
//    val query = tableEvent join table on(_.id === _.eventId)
    execute(seq.result)
//    execute(query.result).map((seq: Seq[(MapEvent, UserJoinEvent)]) => seq.map(tuple => tuple._1))
  }

  override def get(r: Int): Future[UserJoinEvent] = ???
}

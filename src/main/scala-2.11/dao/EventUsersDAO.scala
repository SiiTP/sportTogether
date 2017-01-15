package dao


import java.sql.Timestamp

import com.typesafe.scalalogging.Logger
import entities.db._
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
/**
  * Created by ivan on 15.10.16.
  */
class EventUsersDAO extends DatabaseDAO[UserJoinEvent, Int]{
  private val table = Tables.eventUsers
  private val events = Tables.events
  private val users = Tables.users

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
    val q = table.filter(uie => uie.eventId === eId && uie.userId === userId).delete
    execute(q)
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

  def getEvents(): Future[Seq[(Int, String,Int,String)]] = {
    implicit val getEventResult = GetResult[MapEvent](EventsDAO.mapResult)
    val date = System.currentTimeMillis() / 1000
    val query = sql"""SELECT eu.event_id, eu.device_token, eu.user_id, e.name from events e
           join event_users eu on eu.event_id=e.id
           join user u on u.id=eu.user_id where TIMESTAMPDIFF(SECOND,u.remind_time,e.date) < $date and eu.notified=FALSE """.as[(Int, String,Int,String)]
    execute(query)
  }

  def updateNotified(eventsId: Seq[(Int,Int)]) = {
    val queries = eventsId.map((item:(Int,Int))=> {
      val query = for {uie <- table if uie.eventId === item._1 && uie.userId === item._2} yield uie.notified
      query.update(true)
    })
    execute(DBIO.sequence(queries))
  }

  override def get(r: Int): Future[UserJoinEvent] = ???
}

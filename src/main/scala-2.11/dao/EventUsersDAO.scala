package dao

import com.typesafe.scalalogging.Logger
import entities.db._
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 15.10.16.
  */
class EventUsersDAO extends DatabaseDAO[UserJoinEvent, Int]{

  private val table = Tables.eventUsers
  private val tableEvent = Tables.events
  private val eventsDAO = new EventsDAO()
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

  def getEventsOfUserJoined(user: User): Future[Seq[MapEventAdapter]] = {
    val idUser = user.id.getOrElse(0)
    val seq = for {
      (e, rel) <- tableEvent join table on (_.id === _.eventId ) if rel.userId === idUser
    } yield e
    val result: Future[Seq[MapEvent]] = execute(seq.result)
    result.map(seq => seq.map(mapEvent => MapEventAdapter(
      mapEvent.name,
      MapCategory("", Some(mapEvent.categoryId)),
      mapEvent.latitude,
      mapEvent.longtitude,
      mapEvent.date,
      eventsDAO.getCountUsersInEvent(mapEvent.id.getOrElse(0)),
      mapEvent.maxPeople,
      mapEvent.reports,
      mapEvent.description,
      mapEvent.isEnded,
      mapEvent.userId,
      mapEvent.id
    )))
  }

  override def get(r: Int): Future[UserJoinEvent] = ???
}

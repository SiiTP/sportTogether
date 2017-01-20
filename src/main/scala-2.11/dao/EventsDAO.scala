package dao


import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}
import com.typesafe.scalalogging.Logger
import dao.filters.EventFilters
import slick.backend.StaticDatabaseConfig
import slick.driver.MySQLDriver.api._
import entities.db._
import slick.jdbc.{GetResult, PositionedResult}

import scala.concurrent.duration._
import scala.concurrent.duration.Duration

import scala.concurrent.{Await, Future, duration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * Created by ivan on 19.09.16.
  */
class EventsDAO extends DatabaseDAO[MapEvent,Int] {
  private val table = Tables.events
  private val logger = Logger("webApp")

  private val reportsTable = Tables.userReports
  override def create(r: MapEvent): Future[MapEvent] = {
    val c = r.copy(reports = Some(0), currentUsers = Some(0))
    val insert = (table returning table.map(_.id)).into((item, id) => item.copy(id = Some(id)))
    execute(insert += c)
  }

  override def update(r: MapEvent): Future[Int] = {
    //    val query = table.filter(_.id === r.id)
    val query = for {c <- table if c.id === r.id} yield (c.date, c.description, c.result, c.latitude, c.longtitude, c.isEnded, c.catId, c.userId, c.name)
    val action = query.update((r.date, r.description, r.result, r.latitude, r.longtitude, r.isEnded, r.categoryId, r.userId.get, r.name))
    execute(action)
  }

  override def get(eventId: Int): Future[MapEvent] = execute(table.filter(_.id === eventId).result.head)

  override def delete(r: MapEvent): Future[Int] = execute(table.filter(_.id === r.id).delete)

  def endEvent(id: Int, userId: Int) = {
//    val query = for {c <- table if c.userId === userId && c.id === id} yield c
    execute(table.filter(event => event.id === id && event.userId === userId).delete)
  }

  def reportEvent(id: Int, user: User): Future[Int] = {

    execute(reportsTable += UserReport(user.id.get, id)).flatMap(action => {
      get(id)
    }).flatMap(event => {
      val query = for {e <- table if e.id === event.id} yield e.report
      execute(query.update(event.reports.get + 1))
    })
  }

  def eventsByUserId(id: Int): Future[Seq[MapEvent]] = {
    execute(table.filter(_.userId === id).result)
  }

  def getEvents(filters: EventFilters) = {
    val query = table
    val newQuery = filters.createQueryWithFilter(query).filter(_.isExpired === false).take(150).result
    execute(newQuery)
  }
  def updateEventsStatus() = {
    val timestamp = new Timestamp(Instant.now.minus(1, ChronoUnit.DAYS).toEpochMilli)
    val query = for {c <- table if c.date < timestamp && c.isExpired === false} yield c.isExpired
    val action = query.update(true)
    execute(action)
  }
  def incUsersNow(eventId: Int) = {
     val query = sql"""UPDATE events set users_now = users_now + 1 where events.id = $eventId""".as[Int]
     execute(query)
  }
  def decUsersNow(eventId: Int) = {
    val query = sql"""UPDATE events set users_now = users_now - 1 where events.id = $eventId""".as[Int]
    execute(query)
  }
  def getCountUsersInEvent(idEvent: Option[Int]): Future[Int] = {
    if (idEvent.isEmpty) {
      Future.successful(0)
    }
    val countFuture: Future[Int] = execute(Tables.eventUsers.filter(_.eventId === idEvent).countDistinct.result)
    countFuture.recoverWith {
      case e: Throwable =>
        println(e.getMessage)
        Future.successful(0)
    }
  }

  def isEventReported(idUser: Option[Int], idEvent: Option[Int]): Future[Boolean] = {
    if (idUser.isEmpty && idEvent.isEmpty) {
      return Future.successful(false)
    }
    val query: Rep[Boolean] = reportsTable.filter(row => row.userId === idUser.get && row.eventId === idEvent.get).exists
    execute(query.result)
  }
  def getUserReportsEventsId(idUser: Option[Int]): Future[Seq[Int]] = {
    val query = for { report <- reportsTable if report.userId === idUser} yield report.eventId
    execute(query.result)
  }
  def getNearestEventsByDistance(distance: Double, longtitude: Double, latitude: Double, filters: EventFilters): Future[Seq[MapEvent]] = {
    implicit val getEventResult = GetResult(EventsDAO.mapResult)
    val distanceQuery = new DistanceQuery(distance, longtitude, latitude)
    execute(distanceQuery.distanceQueryEventIds).flatMap[Seq[MapEvent]]((res: Vector[Int]) => {
      val query = table.filter(_.id inSet res)
      execute[Seq[MapEvent]](filters.createQueryWithFilter(query).filter(_.isExpired === false).result)
    })
  }
}
object EventsDAO {
  def mapResult(r: PositionedResult): MapEvent = MapEvent(r.nextString, r.nextInt, r.nextDouble, r.nextDouble, r.nextTimestamp, r.nextInt, r.nextIntOption, r.nextIntOption(), r.nextStringOption, r.nextStringOption, r.nextBoolean, r.nextIntOption, r.nextIntOption)
}
class DistanceQuery(val distance: Double, val longtitude: Double, val latitude: Double) {
  private def longtitudeBetweenTuple = (longtitude - longtitudeDelta,longtitude + longtitudeDelta)
  private def longtitudeDelta = distance/Math.abs(Math.cos(Math.toRadians(latitude)))

  private def latitudeBetweenTuple = (latitude - latitudeDelta, latitude + latitudeDelta)
  private def latitudeDelta = distance/DistanceQuery.LATITUDE_IN_KM


  def distanceQueryEventIds ={
    implicit val getEventResult = GetResult[MapEvent](EventsDAO.mapResult)
    sql"""SELECT id,   1.609344 * 3956 * 2 * ASIN(SQRT( POWER(SIN((${latitude} - abs(events.latitude)) * pi()/180 / 2),2) + COS(${latitude} * pi()/180 ) * COS(abs (events.latitude) *  pi()/180) * POWER(SIN((${longtitude} - events.longtitude) *  pi()/180 / 2), 2) ))   as distance FROM events where latitude between ${latitudeBetweenTuple._1} and ${latitudeBetweenTuple._2} and longtitude between ${longtitudeBetweenTuple._1} and ${longtitudeBetweenTuple._2} having distance < ${distance} order by distance;""".as[Int]
  }
}
object DistanceQuery{
  private val LATITUDE_IN_KM = 111
}



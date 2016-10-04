package dao

import slick.driver.MySQLDriver.api._
import entities.db.{MapEvents, MapCategory, Tables, MapEvent}
import slick.jdbc.GetResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

/**
  * Created by ivan on 19.09.16.
  */
class EventsDAO extends DatabaseDAO[MapEvent,Int]{
  private val table = Tables.events
  override def create(r: MapEvent): Future[MapEvent] = {
    val insert = (table returning table.map(_.id)).into( (item,id) => item.copy(id = Some(id)))
    execute(insert += r)
  }

  override def update(r: MapEvent): Future[Int] = {
    val query = table.filter(_.id === r.id)
    val action = query.update(r)
    execute(action)
  }

  override def get(eventId: Int): Future[MapEvent] = execute(table.filter(_.id === eventId).result.head)

  override def delete(r: MapEvent): Future[Int] = execute(table.filter(_.id === r.id).delete)

  def eventsByCategoryId(r:MapCategory):Future[Seq[MapEvent]] = {
    val seq = for {
      item <- table if item.catId === r.id
    } yield item
    execute(seq.result)
  }
  def eventsByCategoryName(r:MapCategory):Future[Seq[MapEvent]] = {
    val seq = for {
      (i,c) <- table join Tables.categories on (_.catId === _.id) if c.name === r.name
    } yield i
    execute(seq.result)
  }
  def eventsByUserId(id:Int): Future[Seq[MapEvent]] = {
    execute(table.filter(_.userId === id).result)
  }
  def allEvents() = {
    execute(table.result)
  }
  def getEventsByCategoryName(categoryName: String) = {
    val query = table join Tables.categories on (_.catId === _.id)
    execute(query.filter(_._2.name === categoryName).result)
  }
  def getNearestEventsByDistance(distance:Double, longtitude: Double, latitude: Double) = {
    implicit val getEventResult = GetResult(r => MapEvent(r.nextString,r.nextInt, r.nextDouble, r.nextDouble,r.nextTimestamp,r.nextInt,r.nextInt,r.nextStringOption,r.nextIntOption,r.nextIntOption))
    val distanceQuery = new DistanceQuery(distance, longtitude, latitude)
    execute(distanceQuery.distanceQuery)
  }
}

class DistanceQuery(val distance: Double, val longtitude: Double, val latitude: Double) {
  private def longtitudeBetweenTuple = (longtitude - longtitudeDelta,longtitude + longtitudeDelta)
  private def longtitudeDelta = distance/Math.abs(Math.cos(Math.toRadians(latitude)))

  private def latitudeBetweenTuple = (latitude - latitudeDelta, latitude + latitudeDelta)
  private def latitudeDelta = distance/DistanceQuery.LATITUDE_IN_KM

  def distanceQuery ={
    implicit val getEventResult = GetResult[MapEvent](r => MapEvent(r.nextString,r.nextInt, r.nextDouble, r.nextDouble,r.nextTimestamp,r.nextInt,r.nextInt,r.nextStringOption,r.nextIntOption,r.nextIntOption))
    sql"""SELECT *,   1.609344 * 3956 * 2 * ASIN(SQRT( POWER(SIN((${latitude} - abs(events.latitude)) * pi()/180 / 2),2) + COS(${latitude} * pi()/180 ) * COS(abs (events.latitude) *  pi()/180) * POWER(SIN((${longtitude} - events.longtitude) *  pi()/180 / 2), 2) ))   as distance FROM events where latitude between ${latitudeBetweenTuple._1} and ${latitudeBetweenTuple._2} and longtitude between ${longtitudeBetweenTuple._1} and ${longtitudeBetweenTuple._2} having distance < ${distance} order by distance;""".as[MapEvent]
  }
}
object DistanceQuery{
  private val LATITUDE_IN_KM = 111
}


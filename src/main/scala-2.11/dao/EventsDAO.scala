package dao


import dao.filters.EventFilters
import slick.driver.MySQLDriver.api._
import entities.db._
import slick.jdbc.{PositionedResult, GetResult}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

/**
  * Created by ivan on 19.09.16.
  */
class EventsDAO extends DatabaseDAO[MapEvent,Int]{
  private val table = Tables.events
  private val reportsTable = Tables.userReports

  override def create(r: MapEvent): Future[MapEvent] = {
    val c = r.copy(reports = Some(0))
    val insert = (table returning table.map(_.id)).into( (item,id) => item.copy(id = Some(id)))
    execute(insert += c)
  }

  override def update(r: MapEvent): Future[Int] = {
//    val query = table.filter(_.id === r.id)
    val query = for {c <- table if c.id === r.id} yield (c.date, c.description, c.latitude, c.longtitude, c.isEnded, c.catId, c.userId, c.name)
    val action = query.update((r.date, r.description, r.latitude, r.longtitude, r.isEnded, r.categoryId, r.userId.get, r.name))
    execute(action)
  }

  override def get(eventId: Int): Future[MapEvent] = execute(table.filter(_.id === eventId).result.head)

  override def delete(r: MapEvent): Future[Int] = execute(table.filter(_.id === r.id).delete)
  def reportEvent(id: Int, user: User) = {

    execute(reportsTable += UserReport(user.id.get, id)).flatMap(action =>{
      get(id)
    }).flatMap( event => {
      val query = for { e <- table if e.id === event.id } yield e.report
      execute(query.update(event.reports.get + 1))
    })
  }

  def eventsByCategoryId(id: Int): Future[Seq[MapEvent]] = {
    val seq = for {
      item <- table if item.catId === id
    } yield item
    table.join(Tables.categories).on(_.catId === _.id)
    execute(seq.result)
  }

  def eventsByUserId(id:Int): Future[Seq[MapEvent]] = {
    execute(table.filter(_.userId === id).result)
  }
  def getEvents(filters: EventFilters) = {
    val query = table
    val newQuery = filters.createQueryWithFilter(query).result
    execute(newQuery)
  }
  def getEventsByCategoryName(categoryName: String) = {
    val query = table join Tables.categories on (_.catId === _.id)
    execute(query.filter(_._2.name === categoryName).result)
  }
  def getNearestEventsByDistance(distance:Double, longtitude: Double, latitude: Double, filters: EventFilters): Future[Seq[MapEvent]] = {
    implicit val getEventResult = GetResult(EventsDAO.mapResult)
    val distanceQuery = new DistanceQuery(distance, longtitude, latitude)
    execute(distanceQuery.distanceQueryEventIds).flatMap[Seq[MapEvent]]((res: Vector[Int]) => {
      val query = table.filter(_.id inSet res)
      execute[Seq[MapEvent]](filters.createQueryWithFilter(query).result)
    })
  }
//  def test(distance:Double, longtitude: Double, latitude: Double, arr: QueryConditions[MapEvent, MapEvents]) : Future[Seq[MapEvent]] = {
//    implicit val getEventResult = GetResult(EventsDAO.mapResult)
//    val distanceQuery = new DistanceQuery(distance, longtitude, latitude)
//    execute(distanceQuery.distanceQueryEventIds).flatMap[Seq[MapEvent]]((res: Vector[Int]) => {
//      val q = table.filter(_.id inSet res)
//      val newQuery = arr.buildQueryWithConditions(q).result
//      println(newQuery.statements)
//      execute[Seq[MapEvent]](newQuery)
//    })
//  }
}
//object Test extends App{
//  val e = new EventsDAO()
//  val filter = new EventFilters(Map(
//    ("events:name"->"HIMKI")))
//  val builder = filter.createQueryConditionsBuilder
//  var res = Await.result(e.test(50,37.7609,55.9168,builder),Duration.Inf)
//  var res1 = Await.result(e.getNearestEventsByDistance(50,37.7609,55.9168),Duration.Inf)
//  println(res)
//  println(res1)
//
//}
object EventsDAO {
  def mapResult(r: PositionedResult): MapEvent = MapEvent(r.nextString, r.nextInt, r.nextDouble, r.nextDouble, r.nextTimestamp, r.nextInt, r.nextIntOption, r.nextStringOption, r.nextBoolean, r.nextIntOption, r.nextIntOption)
}
class DistanceQuery(val distance: Double, val longtitude: Double, val latitude: Double) {
  private def longtitudeBetweenTuple = (longtitude - longtitudeDelta,longtitude + longtitudeDelta)
  private def longtitudeDelta = distance/Math.abs(Math.cos(Math.toRadians(latitude)))

  private def latitudeBetweenTuple = (latitude - latitudeDelta, latitude + latitudeDelta)
  private def latitudeDelta = distance/DistanceQuery.LATITUDE_IN_KM

  def distanceQuery ={
    implicit val getEventResult = GetResult[MapEvent](EventsDAO.mapResult)
    sql"""SELECT *,   1.609344 * 3956 * 2 * ASIN(SQRT( POWER(SIN((${latitude} - abs(events.latitude)) * pi()/180 / 2),2) + COS(${latitude} * pi()/180 ) * COS(abs (events.latitude) *  pi()/180) * POWER(SIN((${longtitude} - events.longtitude) *  pi()/180 / 2), 2) ))   as distance FROM events where latitude between ${latitudeBetweenTuple._1} and ${latitudeBetweenTuple._2} and longtitude between ${longtitudeBetweenTuple._1} and ${longtitudeBetweenTuple._2} having distance < ${distance} order by distance;""".as[MapEvent]
  }

  def distanceQueryEventIds ={
    implicit val getEventResult = GetResult[MapEvent](EventsDAO.mapResult)
    sql"""SELECT id,   1.609344 * 3956 * 2 * ASIN(SQRT( POWER(SIN((${latitude} - abs(events.latitude)) * pi()/180 / 2),2) + COS(${latitude} * pi()/180 ) * COS(abs (events.latitude) *  pi()/180) * POWER(SIN((${longtitude} - events.longtitude) *  pi()/180 / 2), 2) ))   as distance FROM events where latitude between ${latitudeBetweenTuple._1} and ${latitudeBetweenTuple._2} and longtitude between ${longtitudeBetweenTuple._1} and ${longtitudeBetweenTuple._2} having distance < ${distance} order by distance;""".as[Int]
  }
}
object DistanceQuery{
  private val LATITUDE_IN_KM = 111
}


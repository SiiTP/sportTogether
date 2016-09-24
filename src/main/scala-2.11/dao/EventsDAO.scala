package dao

import slick.driver.MySQLDriver.api._
import entities.db.{MapEvents, MapCategory, Tables, MapEvent}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
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
}

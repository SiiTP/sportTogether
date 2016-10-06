package dao

import entities.db.{MapEvent, MapEvents, Tables, MapCategory}
import slick.driver.MySQLDriver.api._
import scala.concurrent.Future

/**
  * Created by ivan on 20.09.16.
  */
class CategoryDAO extends DatabaseDAO[MapCategory,Int]{
  private val table = Tables.categories
  override def create(r: MapCategory): Future[MapCategory] = {
    val insert = (table returning table.map(_.id)).into( (item,id) => item.copy(id = Some(id)))
    execute(insert += r)
  }
  def eventsByCategoryName(name: String): Future[Seq[MapEvent]] = {
    val seq = for {
      (i,c) <- table join Tables.events on (_.id === _.catId  ) if i.name === name
    } yield c
    execute(seq.result)
  }
  override def update(r: MapCategory): Future[Int] = {
    val query = table.filter(_.id === r.id)
    val action = query.update(r)
    execute(action)
  }

  override def get(categoryId: Int): Future[MapCategory] = execute(table.filter(_.id === categoryId).result.head)

  override def delete(r: MapCategory): Future[Int] = execute(table.filter(_.id===r.id).delete)
  def getCategoryByName(name: String) = execute(table.filter(_.name === name).result)
  def getCategories = execute(table.result)
}

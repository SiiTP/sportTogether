package dao

import dao.filters.CategoryFilters
import entities.db.{MapCategory, MapEvent, MapEvents, Tables}
import slick.driver.MySQLDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global


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

  def getCategories(filter: CategoryFilters) = {
    execute(filter.createQueryWithFilter(table).result)
  }

  def getCategoryByName(name: String) = execute(table.filter(_.name === name).result)

  def getCategoriesByPartOfName(name: String): Future[Seq[MapCategory]] = {
    val query = for {
      e <- table if e.name like s"%$name%"
    } yield e
    execute(query.result).recoverWith {
      case e: NoSuchElementException =>
        println("no categories")
        Future.successful(Seq[MapCategory]())
    }
  }

  override def update(r: MapCategory): Future[Int] = {
    val query = table.filter(_.id === r.id)
    val action = query.update(r)
    execute(action)
  }
  def getCategoriesByIds(ids: Seq[Int]): Future[Seq[MapCategory]] = {
    execute(table.filter(_.id inSet  ids).result)
  }
  override def get(categoryId: Int): Future[MapCategory] = execute(table.filter(_.id === categoryId).result.head)
  override def delete(r: MapCategory): Future[Int] = execute(table.filter(_.id===r.id).delete)
}

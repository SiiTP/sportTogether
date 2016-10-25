package dao.filters

import entities.db._
import slick.driver.MySQLDriver.api._
/**
  * Created by ivan on 25.10.16.
  */
abstract class Test[E, T <: Table[E]](_paramMap: Map[String,String], table: TableQuery[T]) {
  def createQuery :Query[T,T#TableElementType, scala.Seq] = {
    var query: Query[T, T#TableElementType, scala.Seq] = table
    _paramMap.foreach((a:(String,String)) => {
      getFilter(a._1) match {
        case Some(f) =>
          query = query.filter(f(a._2))
      }
    })
    println(query.result.statements)
    query
  }
  def getFilter(name: String): Option[(String) => T => Rep[Boolean]]
}
class EventFiltersConatiner(_paramMap: Map[String,String]) extends Test[MapEvent, MapEvents](_paramMap,EventFiltersConatiner.table){

  def getFilter(name: String) = EventFiltersConatiner.filters.get(name)

}
class CategoryFiltersContainer(_paramMap: Map[String,String]) extends Test[MapCategory, MapCategories](_paramMap,CategoryFiltersContainer.table){

  def getFilter(name: String) = CategoryFiltersContainer.filters.get(name)

}
object EventFiltersConatiner {
  private val table = Tables.events
  private val filters = scala.collection.immutable.Map(
    ("name", (name: String) => (f:MapEvents) => f.name === name),
    ("report", (name: String) => {
      val rep = Integer.parseInt(name)
      (f:MapEvents) => f.report === rep
    }),
    ("ge:report", (name: String) => {
      val rep = Integer.parseInt(name)
      (f:MapEvents) => f.report >= rep
    })

  )
}
object CategoryFiltersContainer {
  private val table = Tables.categories
  private val filters = scala.collection.immutable.Map(
    ("name", (name: String) => (f:MapCategories) => f.name === name),
    ("contains:name", (name: String) => {
      (f:MapCategories) => f.name like name
    })
  )
}
object f extends App {
  val e = new EventFiltersConatiner(Map(
    ("name"->"vasya"),
    ("report"->"25"),
    ("ge:report"->"25")
  ))
  val query = e.createQuery
  val ee = new CategoryFiltersContainer(Map(
    ("name"->"vasya"),
    ("contains:name"->"25")
  ))
//  var res = (Tables.events ++ query).join(Tables.categories).on(_.catId == _.name)
  var res = Tables.events.
  println(res.result.statements)
  ee.createQuery
}

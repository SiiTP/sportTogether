package dao.filters

import entities.db._
import slick.dbio.DBIOAction
import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by ivan on 25.10.16.
  */
abstract class ParametersFiltration[E, T <: Table[E]](_paramMap: Map[String,List[String]], table: TableQuery[T]) {

  private def parseConditions : ArrayBuffer[(T) => Rep[Boolean]]= {
    val where = new ArrayBuffer[(T) => Rep[Boolean]]()
    _paramMap.foreach((a:(String,List[String])) => {
      getFilter(a._1) match {
        case Some(f) =>
          where += f(a._2)
      }
    })
    where
  }

  def createQueryConditions = new QueryConditions[E,T](parseConditions)
  protected def getFilter(name: String): Option[(List[String]) => T => Rep[Boolean]]
}
class QueryConditions[E,T <: Table[E]](_arr: ArrayBuffer[(T) => Rep[Boolean]]) {
  def buildQueryWithConditions(q: Query[T,T#TableElementType,Seq]): Query[T,T#TableElementType,Seq] = {
    var result = q
    _arr.foreach(cond => {
      result = result.filter[Rep[Boolean]](cond)
    })
    result
  }
}
class EventFilters(_paramMap: Map[String,List[String]]) extends ParametersFiltration[MapEvent, MapEvents](_paramMap,EventFilters.table){

  def getFilter(name: String) = EventFilters.filters.get(name)
  def getTableJoinFilter(name: String) = EventFilters.crossJoinFilters.get(name)
}

object EventFilters {
  private val table = Tables.events
  private val filters = scala.collection.immutable.Map(
    ("events:name", (values: List[String]) => {
      if (values.size == 1)
        (f:MapEvents) => f.name === values.head
      else
        (f:MapEvents) => f.name inSet  values

    }),
    ("events:report", (values: List[String]) => {
      if (values.size == 1)
        (f:MapEvents) => f.report === values.head.toInt
      else
        (f:MapEvents) => f.report inSet  values.map(_.toInt)
    }),
    ("events:ge:report", (values: List[String]) => {
        val rep = values.head.toInt
        (f:MapEvents) => f.report >= rep
    })
  )
  private val crossJoinFilters = scala.collection.immutable.Map(
    ("events:category:name", (values: List[String]) => {
      table.join(Tables.categories).on(_.catId === _.id).filter(_._2.name inSet values).map(_._1)
    })
  )
}
//class CategoryFiltersContainer(_paramMap: Map[String,String]) extends ParametersFiltration[MapCategory, MapCategories](_paramMap,CategoryFiltersContainer.table){
//
//  def getFilter(name: String) = CategoryFiltersContainer.filters.get(name)
//
//}

object CategoryFiltersContainer {
  private val table = Tables.categories
  private val filters = scala.collection.immutable.Map(
    ("category:name", (name: String) => (f:MapCategories) => f.name === name),
    ("category:contains:name", (name: String) => {
      (f:MapCategories) => f.name like name
    })
  )
}
object f extends App {
//  val e = new EventFilters(Map(
//    ("events:name"->"vasya"),
//    ("events:report"->"25"),
//    ("events:ge:report"->"25")
//  ))
//  var tt = Tables.events.filter(_.id > 0)
//  val builder = e.createQueryConditions
//  tt = builder.buildQueryWithConditions(tt)
//  println(tt.result.statements)
//  println("__________________")
//  var res = Tables.events.to[scala.Seq]
//
//  val j = res.join(Tables.categories).on(_.catId === _.id)
//  println(j.result.statements)
//  val ee = new CategoryFiltersContainer(Map(
//    ("category:name"->"vasya"),
//    ("category:contains:name"->"25")
//  ))
//  val jj = Tables.events.join(Tables.categories).on(_.catId === _.id)
//  println(jj.result.statements)
//  var res = (Tables.events ++ query).join(Tables.categories).on(_.catId == _.name)

  //______JOINS_______________
//  val table = Tables.events
//  val values = Traversable(30, 31, 32, 33)
//  val q = table.join(Tables.categories).on(_.catId === _.id).filter(_._1.id inSet values).map(_._1).joinLeft(Tables.eventUsers).on(_.id === _.eventId).map(_._1).result
//  val q2 = table.join(Tables.categories).on(_.catId === _.id).filter(_._1.id inSet values).joinLeft(Tables.eventUsers).on(_._1.id === _.eventId).map(_._1._1).result
//  println(q.statements)
//  println(q2.statements)
//  var res = Await.result(DatabaseExecutor.getInstance.run(q2),Duration.Inf)
//  var res2 = Await.result(DatabaseExecutor.getInstance.run(q),Duration.Inf)
//  var res3 = Await.result(DatabaseExecutor.getInstance.run(table.filter(_.id inSet values).result),Duration.Inf)
//  println(res3)
//  println(res)
//  println(res2)
  //____________________________
  val s = "event:category:name"
  s.split(":")
//  ee.createQuery
}

package dao.filters

import entities.db._
import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by ivan on 25.10.16.
  */
abstract class ParametersFiltration[E, T <: Table[E]](_paramMap: Map[String,List[String]], table: TableQuery[T]) {

  private def createConditions : ArrayBuffer[(T) => Rep[Boolean]]= {
    val where = new ArrayBuffer[(T) => Rep[Boolean]]()
    _paramMap.foreach((a:(String,List[String])) => {
      getFilter(a._1) match {
        case Some(f) =>
          where += f(a._2)
      }
    })
    where
  }
  def createQueryConditionsBuilder = new QueryConditionsBuilder[E,T](createConditions)
  protected def getFilter(name: String): Option[(List[String]) => T => Rep[Boolean]]
}
class QueryConditionsBuilder[E,T <: Table[E]](_arr: ArrayBuffer[(T) => Rep[Boolean]]) {
  def buildQueryWithConditions(q: Query[T,T#TableElementType,Seq]) = {
    var result = q
    _arr.foreach(cond => {
      result = result.filter[Rep[Boolean]](cond)
    })
    result
  }
}
class EventFiltersConatiner(_paramMap: Map[String,List[String]]) extends ParametersFiltration[MapEvent, MapEvents](_paramMap,EventFiltersConatiner.table){

  def getFilter(name: String) = EventFiltersConatiner.filters.get(name)

}

object EventFiltersConatiner {
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
  val e = new EventFiltersConatiner(Map(
    ("events:name"->"vasya"),
    ("events:report"->"25"),
    ("events:ge:report"->"25")
  ))
  var tt = Tables.events.filter(_.id > 0)
  val builder = e.createQueryConditionsBuilder
  tt = builder.buildQueryWithConditions(tt)
  println(tt.result.statements)
  println("__________________")
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

//  ee.createQuery
}

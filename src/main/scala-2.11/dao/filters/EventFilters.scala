package dao.filters

import java.lang.reflect.Method
import java.sql.{Date, Timestamp}

import entities.db._
import slick.dbio.DBIOAction
import slick.driver.MySQLDriver
import slick.driver.MySQLDriver.api._
import slick.lifted.ColumnOrdered

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by ivan on 25.10.16.
  */
abstract class ParametersFiltration[E, T <: Table[E]](_paramMap: Map[String,List[String]]) {
//todo add user to constructor
  val tableMap = scala.collection.mutable.Map[String,List[String]]()
  val joinsTableMap = scala.collection.mutable.Map[String,scala.collection.mutable.Map[String,List[String]]]()
  var sortList = Seq[String]()//todo: release in future

  def createQueryWithFilter(q: Query[T,E,Seq]): Query[T,E,Seq] = {
    parseParameters()
    val resultQuery = buildQuerySortConditions(createTableConditions(createJoinTableConditions(q)))
    resultQuery
  }
  private def createJoinTableConditions(q: Query[T,E,Seq]) = {
    var result = q
    joinsTableMap.foreach((f:(String,scala.collection.mutable.Map[String,List[String]])) => {
      result = buildJoinTableQueries(f._1,f._2,result)
    })
    result
  }
  private def createTableConditions(q: Query[T,E,Seq]) = buildTableQueries(tableMap, q)
  private def parseParameters() = {
    _paramMap.foreach((entry:(String,List[String])) => {
      val splited = entry._1.split(":")
      if (splited.length > 1 && splited(0).equals(getFilterName)) {
        splited.length match {
          case 2 =>
            splited(1) match {
              case "sort" =>
                sortList = entry._2
              case _ =>
                tableMap.put(splited(1),entry._2)
            }
          case 3 =>
            addJoinTableItem(splited(1), splited(2), entry._2)
        }
      }

    })
  }

  def getTableSortField(t: T, fieldName: String): Option[Method] = {
    try {
      return Some(t.getClass.getMethod(fieldName))
    } catch {
      case e: NoSuchMethodException =>
        println(e.getMessage)
    }
    None
  }

  protected def buildQuerySortConditions(q: Query[T,E,Seq]): Query[T,E,Seq] = {
    var result = q
    sortList.foreach((item: String) => {
      var isDesc = false
      var fieldName = item
      if (item.contains("Desc")) {
        isDesc = true
        fieldName = item.substring(0,item.indexOf("Desc"))
      }
      //супер костыль
      getTableSortField(getTable.baseTableRow, fieldName) match {
        case Some(field) =>
          result = result.sortBy((f:T) => {
            if(isDesc)
              field.invoke(f).asInstanceOf[Rep[String]].desc
            else
              field.invoke(f).asInstanceOf[Rep[String]].asc
          })
        case None =>
      }
    })
    result
  }

  private def addJoinTableItem(tableName: String, tableFilterName: String, args: List[String]) = {
    joinsTableMap.get(tableName) match {
      case Some(item) =>
        item.put(tableFilterName, args)
      case None =>
        joinsTableMap.put(tableName,scala.collection.mutable.Map((tableFilterName,args)))
    }
  }
  protected def getTable: TableQuery[T]
  protected def getFilterName : String
  protected def buildJoinTableQueries(tableName: String, args: scala.collection.mutable.Map[String,List[String]], q: Query[T,T#TableElementType,Seq]) : Query[T,T#TableElementType,Seq]
  protected def buildTableQueries(args: scala.collection.mutable.Map[String,List[String]], q: Query[T,T#TableElementType,Seq]) : Query[T,T#TableElementType,Seq]
  protected def buildSortQueries(args: scala.collection.mutable.Seq[String], q: Query[T,T#TableElementType,Seq]) : Query[T,T#TableElementType,Seq]
}
object ParametersFiltrationUtil {
  def parseConditions[T](params: scala.collection.mutable.Map[String, List[String]],
                      filterMap: Map[String, (List[String]) => (T) => Rep[Boolean]]
                     ) : ArrayBuffer[(T) => Rep[Boolean]]= {
    val where = new ArrayBuffer[(T) => Rep[Boolean]]()
    params.foreach((a:(String,List[String])) => {
      filterMap.get(a._1) match {
        case Some(f) =>
          where += f(a._2)
        case None =>
      }
    })
    where
  }
//  def parseSortConditions[T](params: Seq[String],
//                             sortMap: Map[String, (T) => ColumnOrdered[Object]]) = {
//    val sorts = new ArrayBuffer[(T) => Rep[Boolean]]()
//    params.foreach((sortField: String) => {
//      sortMap.get(sortField) match {
//        case Some(item) =>
//          sorts += item
//        case None =>
//      }
//    })
//    sorts
//  }


  def buildQueryWithConditions[T,E](q: Query[T,E,Seq],
                                    conditions: Seq[(T) => Rep[Boolean]]): Query[T,E,Seq] = {
    var result = q
    conditions.foreach(cond => {
      result = result.filter[Rep[Boolean]](cond)
    })
    result
  }
}
class EventFilters(_paramMap: Map[String,List[String]]) extends ParametersFiltration[MapEvent, MapEvents](_paramMap){

  override protected def getFilterName: String = "events"
  override protected def buildTableQueries(
                                            args: scala.collection.mutable.Map[String, List[String]],
                                            q: Query[MapEvents, MapEvent, Seq]
                                          ): Query[MapEvents, MapEvent, Seq] = {
    val result = q
    val conditions = ParametersFiltrationUtil.parseConditions[MapEvents](args, EventFilters.filters)
    ParametersFiltrationUtil.buildQueryWithConditions(result, conditions)
  }
  override protected def buildJoinTableQueries(
                                              tableName: String,
                                              args: scala.collection.mutable.Map[String, List[String]],
                                              q: Query[MapEvents, MapEvent, Seq]
                                            ): Query[MapEvents, MapEvent, Seq] = {
    tableName match {
      case "category" =>
        val newQuery = q.join(Tables.categories).on(_.catId === _.id)
        val conds = ParametersFiltrationUtil.parseConditions[(MapEvents, MapCategories)](args, EventFilters.categoryFilters)
        ParametersFiltrationUtil.buildQueryWithConditions[
          (MapEvents, MapCategories),
          (MapEvents#TableElementType, MapCategories#TableElementType)
          ](newQuery, conds).map(_._1)

    }
  }

  override protected def buildSortQueries(args: mutable.Seq[String], q: MySQLDriver.api.Query[MapEvents, MapEvent, Seq]): MySQLDriver.api.Query[MapEvents, MapEvent, Seq] = {
      null
  }

  override protected def getTable: TableQuery[MapEvents] = Tables.events
}

object EventFilters {
  private val filters = scala.collection.immutable.Map(
    ("name", (values: List[String]) => {
      if (values.size == 1)
        (f:MapEvents) => f.name === values.head
      else
        (f:MapEvents) => f.name inSet  values

    }),
    ("catId", (values: List[String]) => {
      if (values.size == 1)
        (f:MapEvents) => f.catId === values.head.toInt
      else
        (f:MapEvents) => f.catId inSet  values.map(_.toInt)

    }),
    ("report", (values: List[String]) => {
      if (values.size == 1)
        (f:MapEvents) => f.report === values.head.toInt
      else
        (f:MapEvents) => f.report inSet  values.map(_.toInt)
    }),
    ("GEreport", (values: List[String]) => {
        val rep = values.head.toInt
        (f:MapEvents) => f.report >= rep
    })
  )
  private val eventsSort = scala.collection.immutable.Map[String,(MapEvents) => ColumnOrdered[String]](
    ("name", (f:MapEvents) => f.name.asc),
    ("nameDesc", (f:MapEvents) => f.name.desc)
  )


  private val categoryFilters = scala.collection.immutable.Map(
    ("name", (values: List[String]) => {
      if (values.size == 1)
        (f:(MapEvents, MapCategories)) => f._2.name === values.head
      else
        (f:(MapEvents, MapCategories)) => f._2.name inSet values
    })
  )
}
//class CategoryFiltersContainer(_paramMap: Map[String,String]) extends ParametersFiltration[MapCategory, MapCategories](_paramMap,CategoryFiltersContainer.table){
//
//  def getFilter(name: String) = CategoryFiltersContainer.filters.get(name)
//
//}

//object CategoryFiltersContainer {
//  private val table = Tables.categories
//  private val filters = scala.collection.immutable.Map(
//    ("category:name", (name: String) => (f:MapCategories) => f.name === name),
//    ("category:contains:name", (name: String) => {
//      (f:MapCategories) => f.name like name
//    })
//  )
//}

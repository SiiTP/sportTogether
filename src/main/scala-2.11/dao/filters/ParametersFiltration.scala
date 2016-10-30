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
  val tableMap = scala.collection.mutable.Map[String,List[String]]()
  val joinsTableMap = scala.collection.mutable.Map[String,scala.collection.mutable.Map[String,List[String]]]()
  var sortList = Seq[String]()

  def createQueryWithFilter(q: Query[T,E,Seq]): Query[T,E,Seq] = {
    parseParameters()
    val resultQuery = createQuerySortConditions(createTableConditions(createJoinTableConditions(q)))
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

  protected def createQuerySortConditions(q: Query[T,E,Seq]): Query[T,E,Seq] = {
    var result = q
    sortList.foreach((item: String) => {
      var isDesc = false
      var fieldName = item
      if (item.contains("Desc")) {
        isDesc = true
        fieldName = item.substring(0,item.indexOf("Desc"))
      }
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
  protected def buildTableQueries(args: scala.collection.mutable.Map[String,List[String]], q: Query[T,T#TableElementType,Seq]) : Query[T,T#TableElementType,Seq] = {
    val result = q
    val conditions = ParametersFiltrationUtil.parseConditions[T](args, getMainTableFiltersMap)
    ParametersFiltrationUtil.buildQueryWithConditions(result, conditions)
  }
  protected def getTable: TableQuery[T]
  /**
    * при парсинге параметров првоеряет по первой части параметра {filterName}:*:*
    * @return
    */
  protected def getFilterName : String

  /**
    * (T) => Rep[Boolean] сам фильтр, подставляется в table.filter(*сюда*)
    * @return
    */
  protected def getMainTableFiltersMap : Map[String, (List[String]) => (T) => Rep[Boolean]]
  /**
    * Эту херню нужео самому переопределять, т.к. join для каждой таблицы по своим полям происходит
    * @param tableName таблицы для join фильтрации
    * @param args
    * @param q
    * @return
    */
  protected def buildJoinTableQueries(tableName: String, args: scala.collection.mutable.Map[String,List[String]], q: Query[T,T#TableElementType,Seq]) : Query[T,T#TableElementType,Seq] = {
    q
  }
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

  def buildQueryWithConditions[T,E](q: Query[T,E,Seq],
                                    conditions: Seq[(T) => Rep[Boolean]]): Query[T,E,Seq] = {
    var result = q
    conditions.foreach(cond => {
      result = result.filter[Rep[Boolean]](cond)
    })
    result
  }
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
//    ("category:nameContains", (name: String) => {
//      (f:MapCategories) => f.name like name
//    })
//  )
//}

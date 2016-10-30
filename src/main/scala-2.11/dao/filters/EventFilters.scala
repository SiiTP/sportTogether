package dao.filters

import entities.db.{MapCategories, Tables, MapEvents, MapEvent}
import slick.driver.MySQLDriver
import slick.lifted.{Query, ColumnOrdered}
import slick.driver.MySQLDriver.api._
import scala.collection.mutable

/**
  * Created by ivan on 30.10.16.
  */
class EventFilters(_paramMap: Map[String,List[String]]) extends ParametersFiltration[MapEvent, MapEvents](_paramMap){
  /**
    * при парсинге параметров првоеряет по первой части параметра {filterName}:*:*
    *
    * @return
    */
  override protected def getFilterName: String = "events"

  /**
    * Эту херню нужео самому переопределять, т.к. join для каждой таблицы по своим полям происходит
    * @param tableName
    * @param args
    * @param q
    * @return
    */
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
  override protected def getTable: TableQuery[MapEvents] = Tables.events
  /**
    * (T) => Rep[Boolean] сам фильтр, подставляется в table.filter(*сюда*)
    *
    * @return
    */
  override protected def getMainTableFiltersMap: Map[String, (List[String]) => (MapEvents) => MySQLDriver.api.Rep[Boolean]] = EventFilters.filters
}

object EventFilters {
  //Фильтры для самой таблицы MapEvents
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
  //для фильтров по смежным таблице MapEvents->MapCategories
  private val categoryFilters = scala.collection.immutable.Map(
    ("name", (values: List[String]) => {
      if (values.size == 1)
        (f:(MapEvents, MapCategories)) => f._2.name === values.head
      else
        (f:(MapEvents, MapCategories)) => f._2.name inSet values
    })
  )
}
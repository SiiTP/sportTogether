package dao.filters

import entities.db._
import slick.driver.MySQLDriver
import slick.driver.MySQLDriver.api._
import scala.collection.mutable

/**
  * Created by ivan on 30.10.16.
  */
class CategoryFilters(_paramMap: Map[String,List[String]]) extends ParametersFiltration[MapCategory, MapCategories](_paramMap) {
  override protected def getTable: MySQLDriver.api.TableQuery[MapCategories] = Tables.categories

  /**
    * при парсинге параметров првоеряет по первой части параметра {filterName}:*:*
    *
    * @return
    */
  override protected def getFilterName: String = "category"
  /**
    * (T) => Rep[Boolean] сам фильтр, подставляется в table.filter(*сюда*)
    *
    * @return
    */
  override protected def getMainTableFiltersMap: Map[String, (List[String]) => (MapCategories) => MySQLDriver.api.Rep[Boolean]] = CategoryFilters.filters
}


object CategoryFilters {
  private val filters = scala.collection.immutable.Map(
    ("name", (values: List[String]) =>{
      if (values.size == 1) {
        (f:MapCategories) => f.name === values.head
      } else {
        (f:MapCategories) => f.name inSet values
      }
    }),
    ("nameContains", (values: List[String]) => {
      (f:MapCategories) => f.name like values.head
    }),
    ("id", (values: List[String]) =>{
      if (values.size == 1) {
        (f:MapCategories) => f.id === values.head.toInt
      } else {
        (f:MapCategories) => f.id inSet values.map(_.toInt)
      }
    })
  )
}
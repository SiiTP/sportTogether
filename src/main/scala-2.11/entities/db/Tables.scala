package entities.db

import slick.driver.MySQLDriver.api._
import slick.lifted.Tag
import spray.json._

/**
  * Created by ivan on 15.09.16.
  */
object Tables {
  var events = TableQuery[MapEvents]
  var categories = TableQuery[MapCategories]
  var users = TableQuery[Users]
}

case class MapCategory(name: String, id: Option[Int] = None)
case class MapEvent(name: String, categoryId: Int,  latitude: Double, longtitude: Double,userId: Option[Int] = None, id: Option[Int] = None)
case class User(clientId: String, role: Int, id: Option[Int] = None)

object EntitiesJsonProtocol extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat3(User)
  implicit val mapEventFormat = jsonFormat6(MapEvent)
  implicit val mapCategoryFormat = jsonFormat2(MapCategory)

}



class MapCategories(tag:Tag) extends Table[MapCategory](tag,"category"){
  def id = column[Int]("cat_id",O.PrimaryKey,O.AutoInc)
  def name = column[String]("name")
  def * = (name,id.?) <> (MapCategory.tupled,MapCategory.unapply)
}
class MapEvents(tag:Tag) extends Table[MapEvent](tag,"events"){
  def id = column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name = column[String]("name")
  def catId = column[Int]("cat_id")
  def userId = column[Int]("user_id")
  def latitude = column[Double]("latitude")
  def longtitude = column[Double]("longtitude")
  def mapCategory = foreignKey("cat_fk",catId,Tables.categories)(_.id)
  def mapUser = foreignKey("user_fk",userId,Tables.users)(_.id)
  def * = (name,catId,latitude,longtitude,userId.?,id.?) <> (MapEvent.tupled,MapEvent.unapply)
}
class Users(tag:Tag) extends Table[User](tag,"user"){
  def id = column[Int]("id",O.PrimaryKey,O.AutoInc)
  def clientId = column[String]("clientId")
  def role = column[Int]("role")
  def * = (clientId, role, id.?) <> (User.tupled,User.unapply)
}
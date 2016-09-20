package entities

import slick.driver.MySQLDriver.api._
import slick.lifted.Tag

/**
  * Created by ivan on 15.09.16.
  */
object Tables {
  var events = TableQuery[MapEvents]
  var categories = TableQuery[MapCategories]
  var users = TableQuery[Users]
}

case class MapCategory(name:String,id:Option[Int] = None)
case class MapEvent(name:String, categoryId:Int,latitude:Double,longtitude:Double,id:Option[Int] = None)
case class User(token:String,role:String)

class MapCategories(tag:Tag) extends Table[MapCategory](tag,"category"){
  def id = column[Int]("cat_id",O.PrimaryKey,O.AutoInc)
  def name = column[String]("name")
  def * = (name,id.?) <> (MapCategory.tupled,MapCategory.unapply)
}
class MapEvents(tag:Tag) extends Table[MapEvent](tag,"events"){
  def id = column[Int]("id",O.PrimaryKey,O.AutoInc)
  def name = column[String]("name")
  def catId = column[Int]("cat_id")
  def latitude = column[Double]("latitude")
  def longtitude = column[Double]("longtitude")
  def mapCategory = foreignKey("cat_fk",catId,Tables.categories)(_.id)
  def * = (name,catId,latitude,longtitude,id.?) <> (MapEvent.tupled,MapEvent.unapply)
}
class Users(tag:Tag) extends Table[User](tag,"user"){
  def token = column[String]("token", O.PrimaryKey)
  def role = column[String]("role")
  def * = (token,role) <> (User.tupled,User.unapply)
}
package entities.db

import java.sql.Timestamp

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
  val userReports = TableQuery[UserReports]
  val eventUsers = TableQuery[UsersInEvents]
  val tasks = TableQuery[EventTasks]
}

case class MapCategory(name: String, id: Option[Int] = None)
case class MapEvent(
                     name: String,
                     categoryId: Int,
                     latitude: Double,
                     longtitude: Double,
                     date: Timestamp,
                     maxPeople: Int = 0,
                     reports: Option[Int] = None,
                     description: Option[String] = None,
                     result: Option[String] = None,
                     isEnded: Boolean = false,
                     userId: Option[Int] = None,
                     id: Option[Int] = None
                   )

case class MapEventAdapter(
                            name: String,
                            category: MapCategory,
                            latitude: Double,
                            longtitude: Double,
                            date: Timestamp,
                            nowPeople: Option[Int] = None,
                            maxPeople: Int = 0,
                            reports: Option[Int] = None,
                            description: Option[String] = None,
                            result: Option[String] = None,
                            tasks: Option[Seq[EventTask]] = None,
                            isEnded: Boolean = false,
                            isJoined: Boolean = false,
                            isReported: Boolean = false,
                            userId: Option[Int] = None,
                            id: Option[Int] = None
                          ) {
  def toMapEvent : MapEvent = {
    MapEvent(name,category.id.get, latitude, longtitude, date, maxPeople, reports, description, result, isEnded, userId, id)
  }
}
case class MapEventResultAdapter(id: Int, result: Option[String])
case class EventTask(message:String, eventId:Option[Int] = None, userId:Option[Int] = None, id:Option[Int] = None)
case class User(clientId: String, role: Int, id: Option[Int] = None)
case class UserReport(userId: Int, eventId: Int)
case class UserJoinEvent(userId: Int, deviceToken: String, eventId: Int)
object EntitiesJsonProtocol extends DefaultJsonProtocol {

  implicit  object TimeJsonProtocol extends RootJsonFormat[Timestamp] {
    override def read(json: JsValue): Timestamp = new Timestamp(json.convertTo[Long])
    override def write(obj: Timestamp): JsValue = JsNumber(obj.getTime)
  }

  implicit val userFormat = jsonFormat3(User)
  implicit val tasksFormat = jsonFormat4(EventTask)
  implicit val eventUsersFormat = jsonFormat3(UserJoinEvent)
  implicit val mapEventFormat = jsonFormat12(MapEvent)
  implicit val mapEventResultAdapterFormat = jsonFormat2(MapEventResultAdapter)
  implicit val mapCategoryFormat = jsonFormat2(MapCategory)
  implicit val mapEventAdapterFormat = jsonFormat16(MapEventAdapter)
  implicit val userReportFormat = jsonFormat2(UserReport)
}

class EventTasks(tag: Tag) extends Table[EventTask](tag,"tasks") {
  def id = column[Int]("id",O.PrimaryKey,O.AutoInc)
  def message = column[String]("message")
  def userId = column[Option[Int]]("user_id")
  def eventId = column[Option[Int]]("event_id")
  def userFK = foreignKey("task_userFK",userId,Tables.users)(_.id, onDelete=ForeignKeyAction.Cascade)
  def eventrFK = foreignKey("task_eventFK",eventId,Tables.events)(_.id, onDelete=ForeignKeyAction.Cascade)
  def * = (message, eventId, userId, id.?) <> (EventTask.tupled, EventTask.unapply)
}
class UsersInEvents(tag: Tag) extends Table[UserJoinEvent](tag,"event_users") {
  def userId = column[Int]("user_id")
  def eventId = column[Int]("event_id")
  def deviceToken = column[String]("device_token")
  def userFK = foreignKey("user_UserInEvent_FK",userId,Tables.users)(_.id)
  def eventFK = foreignKey("event_UserInEvent_FK",eventId,Tables.events)(_.id, onDelete=ForeignKeyAction.Cascade)
  def uniqueIdxs = index("uniq_user_in_event",(userId,eventId),unique = true)
  def * = (userId, deviceToken, eventId) <> (UserJoinEvent.tupled, UserJoinEvent.unapply)
}

class UserReports(tag: Tag) extends Table[UserReport](tag,"user_reports") {
  def userId = column[Int]("user_id")
  def eventId = column[Int]("event_id")
  def userFK = foreignKey("userFK",userId,Tables.users)(_.id)
  def eventFK = foreignKey("eventFK",eventId,Tables.events)(_.id, onDelete=ForeignKeyAction.Cascade)
  def uniqueIdxs = index("uniq_user_event",(userId,eventId),unique = true)
  def * = (userId, eventId) <> (UserReport.tupled, UserReport.unapply)
}

class MapCategories(tag:Tag) extends Table[MapCategory](tag,"category") {
  def id = column[Int]("cat_id", O.PrimaryKey,O.AutoInc)
  def name = column[String]("name", O.SqlType("VARCHAR(127)"))
  def * = (name,id.?) <> (MapCategory.tupled, MapCategory.unapply)
  def unqiueIdx = index("idx_uniq_name", name, unique = true)
}

class MapEvents(tag: Tag) extends Table[MapEvent](tag, "events") {
  def id = column[Int]("id", O.PrimaryKey,O.AutoInc)
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  def result = column[Option[String]]("result")
  def catId = column[Int]("cat_id")
  def userId = column[Int]("user_id")
  def maxPeople = column[Int]("people")
  def report = column[Int]("reports")
  def isEnded = column[Boolean]("isEnded")
  def date = column[Timestamp]("date")
  def latitude = column[Double]("latitude")
  def longtitude = column[Double]("longtitude")
  def mapCategory = foreignKey("cat_fk", catId, Tables.categories)(_.id)
  def mapUser = foreignKey("user_fk", userId, Tables.users)(_.id)
  def * = (name, catId, latitude, longtitude, date,  maxPeople, report.?, description, result, isEnded, userId.?, id.?) <> (MapEvent.tupled,MapEvent.unapply)
}

class Users(tag:Tag) extends Table[User](tag,"user"){
  def id = column[Int]("id",O.PrimaryKey,O.AutoInc)
  def clientId = column[String]("clientId")
  def role = column[Int]("role")
  def * = (clientId, role, id.?) <> (User.tupled,User.unapply)
}
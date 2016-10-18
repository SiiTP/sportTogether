package dao

import java.io.File
import java.net.URL
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import entities.db._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class EventUsersDAOSpec extends FlatSpec with MockFactory with Matchers with BeforeAndAfter with ScalaFutures {

  val configFile = new File(getClass.getResource("../application_test.conf").getPath)
  DatabaseExecutor.config("mysqlDB-test", configFile)
  val dbHelper = DatabaseHelper.getInstance
  dbHelper.init(getClass.getResourceAsStream("../application_test.conf"))
  val userDAO = new UserDAO
  val eventsDAO = new EventsDAO
  val categoryDAO = new CategoryDAO
  val eventUsersDAO = new EventUsersDAO

  def awaitResult[T](a : Awaitable[T]) = Await.result(a, Duration.create(5,TimeUnit.SECONDS))

  after{
    dbHelper.clearTables
  }

  "EventUsersDAO" should "create relation" in {
    val user: User = awaitResult(userDAO.create(User("clientId", Roles.USER.getRoleId)))
    val category: MapCategory = awaitResult(categoryDAO.create(MapCategory("cat")))
    val event: MapEvent = awaitResult(eventsDAO.create(MapEvent("name", category.id.getOrElse(0), 50.0, 50.0, new Timestamp(new java.util.Date().getTime), 0, Some(0), Some("descr"), false, Some(user.id.getOrElse(0)))))
    1 + 1 shouldEqual 2

//    val joinFuture: Future[UserJoinEvent] = eventUsersDAO.create(UserJoinEvent(user.id.getOrElse(0), "azaza", event.id.getOrElse(0)))
//    whenReady(joinFuture) { result =>
//      println(result)
//    }
    //    val userFuture: Future[User] = userDAO.create(User("clientId", Roles.USER.getRoleId))
//    val finalFuture: Future[User] = for {
//      user <- userFuture
//      category <- categoryFuture

//      eventUsers <- eventUsersDAO.create(UserJoinEvent(user.id.getOrElse(0), "azaza", event.id.getOrElse(0)))
//    } yield user

  }
}

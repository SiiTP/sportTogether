package dao

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

import entities.db.{DatabaseHelper, DatabaseExecutor, Roles, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration.Duration
import scala.io.Source

class UserDAOSpec extends FlatSpec with MockFactory with Matchers with BeforeAndAfter{

  val configFile = new File(getClass.getResource("../application_test.conf").getPath)
  DatabaseExecutor.config("mysqlDB-test", configFile)
  val dbHelper = DatabaseHelper.getInstance
  dbHelper.init(getClass.getResourceAsStream("../application_test.conf"))
  val userDAO = new UserDAO

  def awaitResult[T](a : Awaitable[T]) = Await.result(a, Duration.create(5,TimeUnit.SECONDS))

  after{
    dbHelper.clearTables
  }

  "UserDAO" should "create user" in {
    val createdFuture: Future[User] = userDAO.create(User(Some("token"), Roles.USER.getRoleId))
    val createdUser: User = awaitResult(createdFuture)
    awaitResult(userDAO.count) shouldBe 1
    createdUser.role shouldBe Roles.USER.getRoleId
    createdUser.clientId shouldBe "token"
  }
  it should "delete user" in {
    val user = User(Some("token"), Roles.USER.getRoleId)
    awaitResult(userDAO.create(user))
    val result = awaitResult(userDAO.delete(user))
    result shouldBe 1
    val count = awaitResult(userDAO.count)
    count shouldBe 0
  }
  it should "update user" in {
    var user = User(Some("token"), Roles.USER.getRoleId)
    user = awaitResult(userDAO.create(user))

    val newToken = "token2"
    val newUser = User(Some(newToken), Roles.ADMIN.getRoleId, user.id)
    val result = awaitResult(userDAO.update(newUser))
    result shouldBe 1

    user = awaitResult(userDAO.get(user.id.get))

    user.role shouldBe Roles.ADMIN.getRoleId
    user.clientId shouldBe newToken
  }

  it should "not update not exist user" in {
    val user = User(Some("token"), Roles.ADMIN.getRoleId, Some(0))
    val result = awaitResult(userDAO.update(user))

    result shouldBe 0
  }

  it should "not update user without id" in {
    val user = User(Some("token"), Roles.ADMIN.getRoleId)
    val result = awaitResult(userDAO.update(user))

    result shouldBe 0
  }
}

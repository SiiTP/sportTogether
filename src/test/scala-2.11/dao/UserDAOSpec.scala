package dao

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

import entities.{Roles, Account, DatabaseHelper, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.{Awaitable, Await}
import scala.concurrent.duration.Duration

/**
  * Created by root on 21.09.16.
  */
class UserDAOSpec extends FlatSpec with MockFactory with Matchers with BeforeAndAfter{

  println(UserDAOSpec.this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath)
  println(UserDAOSpec.this.getClass.getResource("application_test.conf"))
  val configFile = new File(UserDAOSpec.this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath + "application_test.conf")
  DatabaseHelper.config("mysqlDB-test",configFile)
  val dbHelper = new DatabaseHelper()
  dbHelper.init(configFile.getPath)
  val userDAO = new UserDAO

  def awaitResult[T](a : Awaitable[T]) = Await.result(a,Duration.create(5,TimeUnit.SECONDS))

  after{
    dbHelper.clearTables
  }

  "UserDAO" should "create user" in {
    awaitResult(userDAO.create(User("token", Roles.USER.getRoleId)))
    val count = awaitResult(userDAO.count)
    count shouldBe 1
  }

  it should "delete user" in {
    val user = User("token", Roles.USER.getRoleId)
    awaitResult(userDAO.create(user))
    awaitResult(userDAO.delete(user))
    val count = Await.result(userDAO.count,Duration.create(5,TimeUnit.SECONDS))
    count shouldBe 0
  }
  it should "update user" in {
    var user = User("token",Roles.USER.getRoleId)
    user = awaitResult(userDAO.create(user))

    val newToken = "token2"
    val newUser = User(newToken,Roles.ADMIN.getRoleId,user.id)
    awaitResult(userDAO.update(newUser))

    user = awaitResult(userDAO.get(newUser.id.get))

    user.role shouldBe Roles.ADMIN.getRoleId
    user.token shouldBe newToken
  }
}

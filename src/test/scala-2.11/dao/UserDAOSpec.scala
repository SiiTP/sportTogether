package dao

import java.net.URL

import entities.{Account, DatabaseHelper, User}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by root on 21.09.16.
  */
class UserDAOSpec extends FlatSpec with MockFactory with Matchers {
  def initDB() : UserDAO = {
    val resource: URL = getClass.getResource("../application_test.conf")
    DatabaseHelper.ini(resource.getPath)
    new UserDAO
  }

  "UserDAO" should "create user" in {
    val userDAO: UserDAO = initDB()
    userDAO.create(new User("token", "role"))
    val a = 1 + 1
    a shouldEqual 2
  }
}

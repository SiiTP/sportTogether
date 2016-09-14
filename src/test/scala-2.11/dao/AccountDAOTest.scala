package dao

import entities.Account
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
  * Created by root on 15.07.16.
  */
class AccountDAOTest extends FlatSpec with MockFactory with Matchers {
  "AccountDAO" should "create account" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "pass", Account.ROLE_USER, null, 0)
    accountDAO.accounts.size shouldEqual 1
  }

  it should "not add accounts with equal names" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "pass", Account.ROLE_USER, null, 0) shouldEqual true
    accountDAO.create("first", "pass2", Account.ROLE_ADMIN, null, 0) shouldEqual false

    accountDAO.accounts should have size 1
  }

  it should "get all accounts" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "pass", Account.ROLE_USER, null, 0) shouldEqual true
    accountDAO.create("second", "pass", Account.ROLE_USER, null, 0) shouldEqual true
    accountDAO.get should have size 2
  }

  it should "update account" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "pass", Account.ROLE_USER, null, 0)
    accountDAO.get.exists(_.password == "pass") shouldEqual true

    accountDAO.update("first", "pass updated", Account.ROLE_USER, null, 0).isDefined shouldEqual true
    accountDAO.get.exists(_.password == "pass") shouldEqual false
    accountDAO.get.exists(_.password == "pass updated") shouldEqual true
  }

  it should "not update unexist account" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "", "", null, 0)
    accountDAO.update("second", "", "", null, 0).isDefined shouldEqual false
  }

  it should "delete account" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "", "", null, 0) shouldEqual true
    accountDAO.accounts should have size 1
    accountDAO.delete("first")
    accountDAO.accounts should have size 0
  }

  it should "not delete unexist account" in {
    val accountDAO = new AccountDAO()
    accountDAO.create("first", "", "", null, 0) shouldEqual true
    accountDAO.accounts should have size 1
    accountDAO.delete("second")
    accountDAO.accounts should have size 1
  }
}

package dao

import entities.Account
import org.jetbrains.annotations.TestOnly

import scala.collection.mutable


class AccountDAO {
  private val _accounts = new mutable.HashSet[Account]

  @TestOnly
  def accounts = _accounts

  def get = {
    _accounts.toList
  }

  def create(name : String, password : String, role : String, session: String, timeout : Long): Boolean = {
    !(_accounts.size == (_accounts += new Account(name, password, role, session, timeout)).size)
  }

  def update(name : String, password : String, role : String, session : String, timeout : Long): Option[Account] = {
    _accounts.find(_.name == name) match {
      case Some(account) =>
        _accounts -= account
        val updatedAccount = new Account(name, password, role, session, timeout)
        _accounts += updatedAccount
        Some(updatedAccount)
      case None => None
    }
  }

  def delete(name: String): Unit = {
    _accounts -= new Account(name, null, null, null, 0)
  }

  def deleteSession(session : String): Unit = {
    _accounts.find(_.session == session) match {
      case Some(account) => update(account.name, account.password, account.role, null, 0)
    }
  }
}

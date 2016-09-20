package service

import java.util
import java.util.Calendar

import akka.actor.Actor
import akka.actor.Actor.Receive
import dao.AccountDAO
import entities.Account
import org.jetbrains.annotations.TestOnly

class AccountService(val accountDAO: AccountDAO) extends Actor {

  private val _authAccounts = new util.HashMap[String, Account]
  private val _sessionTimeout = new util.HashMap[String, Long]

  @TestOnly
  def authAccounts() = _authAccounts
  @TestOnly
  def sessionTimeout() = _sessionTimeout

  def isAuthorized(session : String) : Integer = {
    val timeout = _sessionTimeout.get(session)
    if (timeout == 0) {
      return AccountService.CODE_NOT_AUTHORIZED
    }

    val now = Calendar.getInstance.getTimeInMillis
    if (now > timeout) {
      unAuthorize(session)
      AccountService.CODE_NOT_AUTHORIZED_TIMEOUT
    } else {
      AccountService.CODE_AUTHORIZED
    }


  }

  def authorize(session : String, name: String, password : String, timeoutTime : Long) : Integer = {
    isAuthorized(session).toInt match {
      case AccountService.CODE_AUTHORIZED => AccountService.CODE_AUTH_ALREADY
      case _ =>
        accountDAO.get.find(_.name == name) match {
          case Some(account) =>
            if (account.password == password) {
              val updatedAccount = accountDAO.update(name, password, account.role, session, timeoutTime)
              _authAccounts.put(session, updatedAccount.get)
              _sessionTimeout.put(session, timeoutTime)
              AccountService.CODE_AUTH_SUCCESSFUL
            } else {
              AccountService.CODE_AUTH_UNSUCCESSFUL
            }

          case None => AccountService.CODE_AUTH_UNSUCCESSFUL
        }
    }
  }

  def authorizePermanently(session: String, name: String, password: String) =
    authorize(session, name, password, Long.MaxValue)

  def unAuthorize(session: String) : Option[Account] = {
    _authAccounts.remove(session) match {
      case account =>
        _sessionTimeout.remove(session)
        accountDAO.deleteSession(session)
        Some(account)
      case null => None
    }
  }

  def register(session: String, name: String, password: String, role: String) : Int = {
    if (accountDAO.create(name, password, role, session, 0)) {
      authorize(session, name, password, AccountService.timeoutNextWeek())
      AccountService.CODE_REG_SUCCESSFUL
    } else {
      AccountService.CODE_REG_ACC_EXIST
    }
  }

  def registerUser(session: String, name: String, password: String) : Int = {
    this.register(session, name, password, Account.ROLE_USER)
  }

  def getAccount(session: String) : Option[Account] = {
    val authorized = isAuthorized(session)
    if (authorized == AccountService.CODE_AUTHORIZED) {
      return Some(_authAccounts.get(session))
    }
    None
  }

  override def receive = {
    case "test" => println("test received")
    case _ => println("other received")
  }
}

object AccountService {
  val CODE_AUTHORIZED = 0

  val CODE_NOT_AUTHORIZED = 1
  val CODE_NOT_AUTHORIZED_TIMEOUT = 2

  val CODE_AUTH_ALREADY = 10
  val CODE_AUTH_SUCCESSFUL = 11
  val CODE_AUTH_UNSUCCESSFUL = 12

  val CODE_REG_SUCCESSFUL = 20
  val CODE_REG_ACC_EXIST = 21

  val AUTH_TIMEOUT_WEEK : Long = 7 * 24 * 60 * 60 * 1000
  def timeoutNextWeek() = Calendar.getInstance().getTimeInMillis + AUTH_TIMEOUT_WEEK
}

package service

import java.util
import java.util.Calendar

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import dao.{AccountDAO, UserDAO}
import entities.Account
import entities.db.User
import org.jetbrains.annotations.TestOnly
import service.AccountService.AccountHello
import service.RouteServiceActor.{Authorize, IsAuthorized, RouteHello}

import scala.collection.immutable.Range.Inclusive

class AccountServiceActor(accountService: AccountService) extends Actor {

  override def receive = {
    case RouteHello(msg) =>
      println(s"Hello from route : $msg")
      val sender1: ActorRef = sender
      (1 to 100000).max(new Ordering[Int] {
        override def compare(x: Int, y: Int): Int = {
          if (x % 2 == 0) {
            return -1
          }
          if (y % 2 == 0) {
            return -1
          }
          0
        }
      })
      sender1 ! AccountHello("I'm account")
    case IsAuthorized(session) => {
      accountService.isAuthorized(session) match {
        case Some(user) => ResponseSuccess[User](_, _, user)
      }
    }
    case Authorize(session, token, id) => println(s"Authorize : $session - $token");
    case _ => println("other received")
  }
}

object AccountService extends AbstractService {
  case class AccountHello(msg: String)
}

class AccountService() {
  lazy val userDAO = new UserDAO()
  private val _authAccounts = new util.HashMap[String, User]

  @TestOnly
  def authAccounts() = _authAccounts

  def isAuthorized(session : String): Option[User] = {
    if (_authAccounts.containsKey(session))
      Some(_authAccounts.get(session))
    else
      None
  }
//
//  def authorize(session : String, name: String, password : String, timeoutTime : Long) : Integer = {
//    isAuthorized(session).toInt match {
//      case AccountService.CODE_AUTHORIZED => AccountService.CODE_AUTH_ALREADY
//      case _ =>
//        accountDAO.get.find(_.name == name) match {
//          case Some(account) =>
//            if (account.password == password) {
//              val updatedAccount = accountDAO.update(name, password, account.role, session, timeoutTime)
//              _authAccounts.put(session, updatedAccount.get)
//              _sessionTimeout.put(session, timeoutTime)
//              AccountService.CODE_AUTH_SUCCESSFUL
//            } else {
//              AccountService.CODE_AUTH_UNSUCCESSFUL
//            }
//
//          case None => AccountService.CODE_AUTH_UNSUCCESSFUL
//        }
//    }
//  }
//
//  def authorizePermanently(session: String, name: String, password: String) =
//    authorize(session, name, password, Long.MaxValue)
//
//  def unAuthorize(session: String) : Option[Account] = {
//    _authAccounts.remove(session) match {
//      case account =>
//        _sessionTimeout.remove(session)
//        accountDAO.deleteSession(session)
//        Some(account)
//      case null => None
//    }
//  }
//
//  def register(session: String, name: String, password: String, role: String) : Int = {
//    if (accountDAO.create(name, password, role, session, 0)) {
//      authorize(session, name, password, AccountService.timeoutNextWeek())
//      AccountService.CODE_REG_SUCCESSFUL
//    } else {
//      AccountService.CODE_REG_ACC_EXIST
//    }
//  }
//
//  def registerUser(session: String, name: String, password: String) : Int = {
//    this.register(session, name, password, Account.ROLE_USER)
//  }
//
//  def getAccount(session: String) : Option[Account] = {
//    val authorized = isAuthorized(session)
//    if (authorized == AccountService.CODE_AUTHORIZED) {
//      return Some(_authAccounts.get(session))
//    }
//    None
//  }
}

package service

import java.util

import akka.actor.{Actor, ActorRef}
import dao.UserDAO
import entities.db.User
import org.jetbrains.annotations.TestOnly
import response.{AccountResponse, MyResponse}
import service.AccountService.AccountHello
import service.RouteServiceActor.{Authorize, IsAuthorized, RouteHello}

import spray.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}

class AccountServiceActor(accountService: AccountService) extends Actor with AccountResponse {

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
    case IsAuthorized(session) =>
      val authorized: Option[User] = accountService.isAuthorized(session)
      authorized match {
        case Some(user) =>
          println("some")
          sender ! "!"
//          responseAlreadyAuthorized.toJson.prettyPrint
//          responseSuccess[User](user.asInstanceOf[User]).toJson.prettyPrint
        case None => sender ! "!!"/*responseNotAuthorized.toJson.prettyPrint*/
      }
    case Authorize(session, token, clientId) => println(s"Authorize : $session - $token");
    case _ => println("other received")
  }
}

object AccountService {
  case class AccountHello(msg: String)
}

class AccountService {
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

  def authorize(session: String, token: String, clientId: String) : Int = {
    isAuthorized(session) match {
      case Some(user) => return AccountResponse.CODE_AUTH_ALREADY
    }

    val userFuture = userDAO.getByClientId(clientId)
    Await.result(userFuture, Duration(2, duration.SECONDS)) match {
      case User(userClientId, role, id) =>

        //TODO if token right else not success
        _authAccounts.put(session, new User(userClientId, role, id))
        MyResponse.CODE_SUCCESS

      case _ => MyResponse.CODE_NOT_SUCCESS
    }

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
  }
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

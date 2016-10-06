package service

import java.lang.Throwable
import java.util

import akka.actor.{Actor, ActorRef}
import dao.UserDAO
import entities.db.{EntitiesJsonProtocol, User}
import org.jetbrains.annotations.TestOnly
import response.{AccountResponse, MyResponse}
import service.AccountService.AccountHello
import service.RouteServiceActor.{Authorize, IsAuthorized, RouteHello, Unauthorize}
import spray.json._
import EntitiesJsonProtocol._
import dispatch.{_}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Success}

class AccountServiceActor(accountService: AccountService) extends Actor with AccountResponse {

  override def receive = {
    case RouteHello(msg) =>
      println(s"Hello from route : $msg")
      sender ! AccountHello("I'm account")

    case IsAuthorized(clientId) =>
      accountService.isAuthorized(clientId) match {
        case Some(user) =>
          sender ! responseSuccess[User](Some(user)).toJson.prettyPrint
        case None => sender ! responseNotAuthorized.toJson.prettyPrint
      }
    case Authorize(token, clientId) =>
      val s = sender
      val future: Future[Int] = accountService.authorize(token, clientId)

      future.onComplete {
        case Success(AccountResponse.CODE_AUTH_ALREADY) => s ! responseAlreadyAuthorized.toJson.prettyPrint
        case Success(MyResponse.CODE_SUCCESS)           => s ! responseSuccess[User](None).toJson.prettyPrint
        case Success(MyResponse.CODE_NOT_SUCCESS)       =>
          println("code not success")
          s ! responseNotSuccess().toJson.prettyPrint
        case Success(_)                                 => s ! responseNotSuccess().toJson.prettyPrint
        case Failure(e) =>
          println("failure")
          s ! responseNotSuccess().toJson.prettyPrint
      }
    case Unauthorize(session) => accountService.unauthorize(session) match {
      case  MyResponse.CODE_SUCCESS             => sender ! responseSuccess[User](None).toJson.prettyPrint
      case  AccountResponse.CODE_NOT_AUTHORIZED => sender ! responseNotAuthorized.toJson.prettyPrint
    }
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

  def authorize(token: String, clientId: String) : Future[Int] = {
    val authorized: Option[User] = isAuthorized(clientId)
    authorized match {
      case Some(user) => return Future.successful(AccountResponse.CODE_AUTH_ALREADY)
      case None =>
    }
    println("token : " + token)
    val response : Future[String] = checkAuth0Token(token)
    val codeResponse: Future[Boolean] = response map (content => {
      println("auth0 response success : " + content)
      true
    }) recover {
      case exc: Throwable =>
        println("handle dispath exception. token not right")
        false
    }
    val userFuture: Future[User] = userDAO.getByClientId(clientId)
    val f = (for {
      isRightToken <- codeResponse
      user <- userFuture
    } yield {
      println("yield")
      isRightToken match {
        case true =>
          println("case true")
          _authAccounts.put(clientId, user.copy())
          MyResponse.CODE_SUCCESS
        case false =>
          println("case false")
          MyResponse.CODE_NOT_SUCCESS
      }
    }) recover {
      case exc: Throwable =>
        println("for handle exc. case _")
        exc.printStackTrace()
        MyResponse.CODE_NOT_SUCCESS
    }
    f
//    userFuture.flatMap {
//      case user =>
//        _authAccounts.put(clientId, user.copy())
//        Future.successful(MyResponse.CODE_SUCCESS)
//    }


//    userFuture.onSuccess {
//      case User(userClientId, role, id) =>
//        _authAccounts.put(session, new User(userClientId, role, id))
//        return MyResponse.CODE_SUCCESS
//
//      case _ => return MyResponse.CODE_NOT_SUCCESS
//    }
  }

  def unauthorize(session: String): Int = {
    isAuthorized(session) match {
      case Some(user) =>
        _authAccounts.remove(session)
        MyResponse.CODE_SUCCESS
      case None => AccountResponse.CODE_NOT_AUTHORIZED
    }
  }

  def checkAuth0Token(token: String): Future[String] = {
    val params = Map("id_token" -> token)
    val req = url("https://x-devel.auth0.com/tokeninfo/") << params
    dispatch.Http.configure(_ setFollowRedirects true)(req.POST OK as.String)
//    Future.successful("Success!!")
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

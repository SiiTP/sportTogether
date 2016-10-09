package service

import java.lang.Throwable
import java.util

import akka.actor.{Actor, ActorRef}
import dao.UserDAO
import entities.db.{EntitiesJsonProtocol, Roles, User}
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
    val auth0ResponseFuture : Future[String] = checkAuth0Token(token)
    val isRightTokenFuture: Future[Boolean] = auth0ResponseFuture map (content => {
      println("auth0 response success : " + content)
      true
    }) recover {
      case exc: Throwable =>
        println("handle dispath exception. token not right")
        false
    }
    isRightTokenFuture.flatMap({
      case true => {
        userDAO.getByClientId(clientId).flatMap {
          case user: User =>
            println("user!!")
            _authAccounts.put(clientId, user.copy())
            Future.successful(MyResponse.CODE_SUCCESS)
        } recoverWith {
          case exc: NoSuchElementException =>
            println("no user in db, creating")
            userDAO.create(User(clientId, Roles.USER.getRoleId)) map {
              case user =>
                _authAccounts.put(clientId, user.copy())
                MyResponse.CODE_SUCCESS
            } recover {
              case exc: Throwable =>
                println("not successful creating")
                exc.printStackTrace()
                MyResponse.CODE_NOT_SUCCESS
            }
          case _ => Future.successful(1)
        }
      }
      case false => Future.successful(MyResponse.CODE_NOT_SUCCESS)
    })
//    val userFuture: Future[User] = userDAO.getByClientId(clientId)
//    (for {
//      isRightToken <- isRightTokenFuture
//      user <- userFuture
//    } yield {
//      println("yield")
//      isRightToken match {
//        case true =>
//          println("case true")
//          _authAccounts.put(clientId, user.copy())
//          MyResponse.CODE_SUCCESS
//        case false =>
//          println("case false")
//          MyResponse.CODE_NOT_SUCCESS
//      }
//    }) recover {
//      case exc: NoSuchElementException =>
//        println("for handle exc. case _")
//        exc.printStackTrace()
//        MyResponse.CODE_NOT_SUCCESS
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
//    val params = Map("id_token" -> token)
//    val req = url("https://x-devel.auth0.com/tokeninfo/") << params
//    dispatch.Http.configure(_ setFollowRedirects true)(req.POST OK as.String)
    Future.successful("Success!!")
  }

  @TestOnly
  def reset() = {
    _authAccounts.clear()
  }
}

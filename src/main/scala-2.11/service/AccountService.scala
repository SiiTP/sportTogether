package service

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
import com.typesafe.scalalogging.Logger
import dispatch._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Success}

class AccountServiceActor(accountService: AccountService) extends Actor {

  override def receive = {
    case RouteHello(msg) =>
      println(s"Hello from route : $msg")
      sender ! AccountHello("I'm account")

    case IsAuthorized(clientId) =>
      accountService.isAuthorized(clientId) match {
        case Some(user) =>
          sender ! AccountResponse.responseSuccess[User](Some(user)).toJson.prettyPrint
        case None => sender ! AccountResponse.responseNotAuthorized.toJson.prettyPrint
      }
    case Authorize(user, token) =>
      val s = sender
      val future: Future[Int] = accountService.authorize(user, token)

      future.onComplete {
        case Success(AccountResponse.CODE_AUTH_ALREADY) => s ! AccountResponse.responseAlreadyAuthorized.toJson.prettyPrint
        case Success(MyResponse.CODE_SUCCESS)           => s ! AccountResponse.responseSuccess[User](accountService.isAuthorized(user.clientId)).toJson.prettyPrint
        case Success(MyResponse.CODE_NOT_SUCCESS)       => s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
        case Success(_)                                 => s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
        case Failure(e) =>
          println("failure")
          s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
      }
    case Unauthorize(session) => accountService.unauthorize(session) match {
      case  MyResponse.CODE_SUCCESS             => sender ! AccountResponse.responseSuccess[User](None).toJson.prettyPrint
      case  AccountResponse.CODE_NOT_AUTHORIZED => sender ! AccountResponse.responseNotAuthorized.toJson.prettyPrint
    }
    case _ => println("other received")
  }
}

object AccountService {
  case class AccountHello(msg: String)
}

class AccountService {
  val logger = Logger("webApp")

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

  def authorize(user: User, token: String) : Future[Int] = {
    val authorized: Option[User] = isAuthorized(user.clientId)
    authorized match {
      case Some(user) => return Future.successful(MyResponse.CODE_SUCCESS)
      case None =>
    }
    val auth0ResponseFuture : Future[String] = checkAuth0Token(token)
    val isRightTokenFuture: Future[Boolean] = auth0ResponseFuture map (content => {
      logger.info(s"success auth0 response : $content")
      true
    }) recover {
      case exc: Throwable =>
        val message: String = exc.getMessage
        logger.info(s"not success auth0 authentitication : $message")
        false
    }
    isRightTokenFuture.flatMap({
      case true => {
        userDAO.getByClientId(user.clientId).flatMap {
          case user: User =>
            logger.info(s"your clientId already exists. Success!")
            _authAccounts.put(user.clientId, user.copy())
            Future.successful(MyResponse.CODE_SUCCESS)
        } recoverWith {
          case exc: NoSuchElementException =>
            userDAO.create(user) map {
              case user =>
                logger.info(s"your clientId is new. You registered. Success!")
                _authAccounts.put(user.clientId, user.copy())
                MyResponse.CODE_SUCCESS
            } recover {
              case exc: Throwable =>
                logger.info(s"error when user creates : " + exc.getMessage)
                MyResponse.CODE_NOT_SUCCESS
            }
          case exc: Throwable =>
            logger.info(s"Unhandled error : " + exc.getMessage)
            exc.printStackTrace()
            Future.successful(MyResponse.CODE_NOT_SUCCESS)
        }
      }
      case false => Future.successful(MyResponse.CODE_NOT_SUCCESS)
    })
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
    Future.successful("Mocked answer!!!")
  }

  @TestOnly
  def reset() = {
    _authAccounts.clear()
  }
}

package service

import java.util

import akka.actor.{Actor, ActorRef}
import com.redis.RedisClientPool
import dao.UserDAO
import entities.db.{UserAdapter, EntitiesJsonProtocol, Roles, User}
import org.jetbrains.annotations.TestOnly
import response.{AccountResponse, MyResponse}
import service.AccountService.{UpdateUserInfo, AccountHello}
import service.RouteServiceActor.{Authorize, IsAuthorized, RouteHello, Unauthorize}
import service.support.RedisSupportService
import spray.json._
import EntitiesJsonProtocol._
import com.typesafe.scalalogging.Logger
import dispatch._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}
import scala.util.{Failure, Success}

class AccountServiceActor(accountService: AccountService) extends Actor {
  val logger = Logger("webApp")
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
        case Success(MyResponse.CODE_SUCCESS)           => s ! AccountResponse.responseSuccess[User](accountService.isAuthorized(user.clientId.get)).toJson.prettyPrint
        case Success(MyResponse.CODE_NOT_SUCCESS)       => s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
        case Success(_)                                 => s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
        case Failure(e) =>
          println("failure",e)
          s ! AccountResponse.responseNotSuccess().toJson.prettyPrint
      }
    case Unauthorize(session) => accountService.unauthorize(session) match {
      case  MyResponse.CODE_SUCCESS             => sender ! AccountResponse.responseSuccess[User](None).toJson.prettyPrint
      case  AccountResponse.CODE_NOT_AUTHORIZED => sender ! AccountResponse.responseNotAuthorized.toJson.prettyPrint
    }
    case UpdateUserInfo(user) =>
      val s = sender()
      accountService.updateUser(user).onComplete {
        case Success(updatedCount) =>
          if (updatedCount > 0) {
            accountService.updateSessionUser(user)
            s ! AccountResponse.responseSuccess(Some(user.copy(clientId = None))).toJson.prettyPrint
          } else {
            s ! AccountResponse.responseUpdateFailed.toJson.prettyPrint
          }
        case Failure(e) =>
          logger.debug("exception",e)
          s ! AccountResponse.unexpectedError.toJson.prettyPrint
      }
    case _ => println("other received")
  }
}

object AccountService {
  case class AccountHello(msg: String)

  case class UpdateUserInfo(user: User)
}

class AccountService(_redisClientPool: RedisClientPool) {
  val logger = Logger("webApp")

  lazy val userDAO = new UserDAO()
//  private val _authAccounts = new util.HashMap[String,User]()
  private val _authAccounts = new RedisSupportService(_redisClientPool)

  @TestOnly
  def authAccounts() = _authAccounts

  def isAuthorized(session : String): Option[User] = {
    if (_authAccounts.containsKey(session)) {
      val userJson : Option[String] =  _authAccounts.get(session)
      userJson match {
        case Some(userJsonString) => Some(userJsonString.parseJson.convertTo[User])
        case None => None
      }
    } else
      None
  }

  def authorize(user: User, token: String) : Future[Int] = {
    val authorized: Option[User] = isAuthorized(user.clientId.get)
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
      case true =>
        userDAO.getByClientId(user.clientId.get).flatMap {
          case storedUser: User =>
            logger.info(s"your clientId already exists. Success!")
            val copiedUser = user.copy(id = storedUser.id, remindTime = storedUser.remindTime)
            userDAO.update(copiedUser)
            _authAccounts.put(user.clientId.get, copiedUser.toJson.prettyPrint)
            Future.successful(MyResponse.CODE_SUCCESS)
        } recoverWith {
          case exc: NoSuchElementException =>
            userDAO.create(user) map {
              case user =>
                logger.info(s"your clientId is new. You registered. Success!")
                _authAccounts.put(user.clientId.get, user.copy().toJson.prettyPrint)
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
  def getUser(id: Int) = {
    userDAO.get(id)
  }
  def updateSessionUser(user: User) = {
    _authAccounts.put(user.clientId.get,user.toJson.prettyPrint)
  }
  def updateUser(user: User) = {
    userDAO.update(user)
  }
  @TestOnly
  def reset() = {
    _authAccounts.clear()
  }
}

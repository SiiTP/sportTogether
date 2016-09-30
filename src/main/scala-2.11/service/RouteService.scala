package service

import java.security.MessageDigest
import java.time.Instant
import java.util.Date

import akka.actor.{Actor, ActorRef}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import entities.db.{EntitiesJsonProtocol, MapEvent, User}
import response.{AccountResponse, MyResponse}
import service.AccountService.AccountHello
import service.RouteServiceActor._

import akka.actor.{ActorRef, Actor}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import entities.db.{MapCategory, EntitiesJsonProtocol, MapEvent, User}
import entities.db.EntitiesJsonProtocol._
import response.AccountResponse
import service.AccountService.AccountHello

import service.RouteServiceActor.{InitEventService, IsAuthorized, RouteHello}
import spray.http.{StatusCodes, HttpCookie}
import spray.routing._
import spray.routing.directives.OnSuccessFutureMagnet

import scala.collection.Searching.Found
import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, duration}

import scala.concurrent.{ExecutionContext, Await, Future, duration}
import spray.json._
import spray.json.DefaultJsonProtocol._
import EntitiesJsonProtocol.mapEventFormat
import spray.httpx.SprayJsonSupport._
import ExecutionContext.Implicits.global
import scala.util
import scala.util.{Failure,Success}

class RouteServiceActor(_accountServiceRef: AskableActorRef, _eventService: AskableActorRef,_categoryService: AskableActorRef) extends Actor with RouteService {
  implicit lazy val timeout = Timeout(10.seconds)
  def actorRefFactory = context
  def accountServiceRef: AskableActorRef = _accountServiceRef
  def eventsServiceRef = _eventService

  override def sendHello = {
    val future: Future[Any] = accountServiceRef ? RouteHello("Q")
    Await.result(future, Duration(2, duration.SECONDS)) match {
      case AccountHello(msg) => s"msg : $msg"
      case _ => s"unhandled message"
    }
  }
  override def sendIsAuthorized(session: String) = {
    println(session)
    accountServiceRef ? IsAuthorized(session)
  }
  override def sendAuthorize(session: String, clientId: String, token: String): Future[Any] = {
    println("send authorize: " + session + " clientId: " + clientId + " token: " + token)
    accountServiceRef ? Authorize(session, clientId, token)
  }
  override def sendUnauthorize(session: String): Future[Any] = {
    accountServiceRef ? Unauthorize(session)
  }


  override def sendGetEvents() = eventsServiceRef ? EventService.GetEvents()
  def receive = handleMessages orElse runRoute(myRoute)

  //это если актору надо принять наши сообщения
  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)

  }
  override def sendAddEvent(event: MapEvent, user: User) = eventsServiceRef ? EventService.AddEvent(event, user)
  override def sendGetUserEvents(id:Int) = eventsServiceRef ? EventService.GetUserEvents(id)

  override def sendGetEvent(id: Int) = eventsServiceRef ? EventService.GetEvent(id)

  override def sendCreateCategory(name: String): Future[Any] = _categoryService ? CategoryService.CreateCategory(name)

  override def sendGetCategory(id: Int): Future[Any] = _categoryService ? CategoryService.GetCategory(id)

  override def sendGetCategories(): Future[Any] = _categoryService ? CategoryService.GetCategories()
}

object RouteServiceActor {
  case class RouteHello(msg: String)

  case class Authorize(session: String, token: String, clientId: String)
  case class IsAuthorized(session: String)
  case class Unauthorize(session: String)
  case class GetAccount(session: String)
  case class CreateAccount(session: String, token: String, role: Int)

  case class InitEventService(service: ActorRef)
}

trait RouteService extends HttpService with AccountResponse {
  def accountServiceRef: AskableActorRef

  def sendHello: String

  def sendIsAuthorized(session: String): Future[Any]

  def sendAuthorize(session: String, clientId: String, token: String): Future[Any]

  def sendUnauthorize(session: String): Future[Any]

  def sendAddEvent(event: MapEvent, user: User): Future[Any]

  def sendGetEvents(): Future[Any]

  def sendGetUserEvents(id: Int): Future[Any]

  def sendGetEvent(id: Int): Future[Any]

  def sendCreateCategory(name: String): Future[Any]

  def sendGetCategory(id: Int): Future[Any]

  def sendGetCategories(): Future[Any]

  def getStringResponse(data: Any) = data.asInstanceOf[String]

  def auth(token: String,clientId: String) = pathPrefix("auth") {
    get {
      onComplete(sendIsAuthorized(token)) {
        case Success(item) => complete(item.asInstanceOf[String])
        case util.Failure(t) => complete("fail")
      }
    } ~
      delete {
        onComplete(sendUnauthorize(token)) {
          case Success(item) => complete(item.asInstanceOf[String])
          case util.Failure(t) => complete("fail")
        }
      } ~
    post {
      onComplete(sendAuthorize(token, clientId, token)) {
        case Success(item) => complete(item.asInstanceOf[String])
        case util.Failure(t) => complete("fail2")
      }
    }
  }
  def category(user: User) = pathPrefix("category") {
    path(IntNumber) {
      id => {
        get {
          onComplete(sendGetCategory(id)) {
            case Success(result) => complete(getStringResponse(result))
            case Failure(t) => complete(t.getMessage)
          }
        }
      }
    } ~
    get {
      onComplete(sendGetCategories()) {
        case Success(result) => complete(getStringResponse(result))
        case Failure(t) => complete(t.getMessage)
      }
    } ~
    post {
      entity(as[MapCategory]) {
        category =>
          onComplete(sendCreateCategory(category.name)) {
            case Success(result) => complete(getStringResponse(result))
          }
      }
    }
  }
  def event(user: User) = pathPrefix("event") {
    path(IntNumber){
      id =>
        get {
          onComplete(sendGetEvent(id)) {
            case Success(result) => complete(getStringResponse(result))
            case Failure(t) => complete(t.getMessage)
          }
        }
    } ~
    pathEnd {
      get {
        onComplete(sendGetEvents()) {
          case Success(items) => complete(getStringResponse(items))
          case Failure(t) => complete(t.getMessage)
        }
      } ~
      post {
        entity(as[MapEvent]) { event =>
          onComplete(sendAddEvent(event,User("fwefwef",entities.db.Roles.USER.getRoleId,Some(1)))) {
            case Success(result) => complete(getStringResponse(result))
            case Failure(t) => complete(t.getMessage)
          }
        }
      }
    } ~
    pathPrefix("user") {
      get {
        onComplete(sendGetUserEvents(user.id.get)) {
          case Success(items) => complete(items.asInstanceOf[String])
          case Failure(t) => complete("failed " + t.getMessage)
        }
      }
    }
  }
  val other = get {
    pathPrefix("hello") {
      path("world") {
        complete("Hello world!")
      } ~
        parameters('foo.?) { (foo) =>
          val str = foo.getOrElse("not defined")
          complete(s"Foo is $str")
        }
    }
  }

  def getRoute = myRoute
  def sessionRequiredRoutes(token: String,clientId:String) = {
    onSuccess(sendIsAuthorized(clientId)){
      case result =>
        JsonParser(result.asInstanceOf[String]).convertTo[ResponseSuccess[User]].data match {
          case Some(u) =>
            event(u) ~
            category(u)
          case None => complete("fail33")
        }
    }
  }
  val authRoutes = headerValueByName("Token") { token =>
    headerValueByName("ClientId") {
      clientId =>
        auth(token,clientId) ~
        sessionRequiredRoutes(token,clientId)
    }
  }
  val myRoute = {
    authRoutes ~
    other
  }
}

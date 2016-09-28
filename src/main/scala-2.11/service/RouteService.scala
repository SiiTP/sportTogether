package service

import akka.actor.FSM.Failure
import akka.actor.{Actor, ActorRef}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import entities.db.{EntitiesJsonProtocol, MapEvent, User}
import response.{AccountResponse, MyResponse}
import service.AccountService.AccountHello
import service.EventService.ResponseEvent
import service.RouteServiceActor._
import spray.routing._
import spray.routing.directives.OnSuccessFutureMagnet

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, duration}

import ExecutionContext.Implicits.global
import scala.util
import scala.util.Success

class RouteServiceActor(_accountServiceRef: AskableActorRef) extends Actor with RouteService with AccountResponse {
  implicit lazy val timeout = Timeout(10.seconds)
  def actorRefFactory = context
  def accountServiceRef: AskableActorRef = _accountServiceRef
  var _eventService: Option[AskableActorRef] = None
  def eventsServiceRef = _eventService

  override def sendHello = {
    val future: Future[Any] = accountServiceRef ? RouteHello("Q")
    Await.result(future, Duration(2, duration.SECONDS)) match {
      case AccountHello(msg) => s"msg : $msg"
      case _ => s"unhandled message"
    }
  }
  override def sendIsAuthorized(session: String) = {
    accountServiceRef ? IsAuthorized(session)
  }
  override def sendAuthorize(session: String, clientId: String, token: String): Future[Any] = {
    accountServiceRef ? Authorize(session, clientId, token)
  }

  override def sendUnauthorize(session: String): Future[Any] = {
    accountServiceRef ? Unauthorize(session)
  }


  def receive = handleMessages orElse runRoute(myRoute)

  //это если актору надо принять наши сообщения
  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)

    case InitEventService(service) => _eventService = Some(service)
  }

  //event service send
  override def sendGetEvents(session: String, someFilter: Int): String = {
    s"events url $session : $someFilter"
  }
  override def sendAddEvent(event: MapEvent, user: User): Future[Any] = {
     _eventService match {
      case Some(service) => service ? EventService.AddEvent(event,user)
    }
  }
}

object RouteServiceActor {
  case class RouteHello(msg: String)

  case class Authorize(session: String, token: String, clientId: String)
  case class IsAuthorized(session: String)
  case class Unauthorize(session: String)
  case class GetAccount(session: String)
  case class CreateAccount(session: String, token: String, role: Int)

  case class InitEventService(service : ActorRef)
}

trait RouteService extends HttpService {
  def accountServiceRef: AskableActorRef
  def sendHello: String
  def sendIsAuthorized(session: String): Future[Any]
  def sendAuthorize(session: String, clientId: String, token: String): Future[Any]
  def sendUnauthorize(session: String): Future[Any]

  //event service send
  def sendAddEvent(event:MapEvent, user:User): Future[Any]
  def sendGetEvents(session: String, someFilter: Int): String

  val auth = pathPrefix("auth") {
    cookie("JSESSIONID") {
      jSession => {
        get {
          onComplete(sendIsAuthorized(jSession.content)) {
            case Success(item) => complete(item.asInstanceOf[String])
            case util.Failure(t) => complete("fail")
          }
        } ~
        post {
          parameters('clientId, 'token) {(clientId, token) =>
            onComplete(sendAuthorize(jSession.content, clientId, token)) {
              case Success(item) => complete(item.asInstanceOf[String])
              case util.Failure(t) => complete("fail")
            }
          }
        } ~
        delete {
          onComplete(sendUnauthorize(jSession.content)) {
            case Success(item) => complete(item.asInstanceOf[String])
            case util.Failure(t) => complete("fail")
          }
        }
      }
    }
  }
  val event = pathPrefix("event"){
    path(IntNumber){
      id =>
        get{
          complete("get event with id" + id)
        }
//        post{
//          entity(as[MapEvent]){ event =>
//            val future = sendAddEvent(event, User("fwefwef", entities.db.Roles.USER.getRoleId,Some(1)))
//            onComplete(future){
//              case Success(item) => {
//                println("complete")
//                complete(item.asInstanceOf[ResponseEvent].event)
//              }
//              case util.Failure(t) =>{
//                println("fail?")
//                complete("fail?")
//              }
//            }
//          }
//        }
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

  def getRoute = myRoute;

  val myRoute = {
    auth ~
    other ~
    event
  }
}

package service


import akka.actor.{ActorRef, Actor}
import akka.pattern.AskableActorRef
import akka.util.Timeout
import entities.db.{EntitiesJsonProtocol, MapEvent, User}
import response.AccountResponse
import service.AccountService.AccountHello
import service.EventService.ResponseEvent
import service.RouteServiceActor.{InitEventService, IsAuthorized, RouteHello}
import spray.routing._
import spray.routing.directives.OnSuccessFutureMagnet

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Await, Future, duration}
import spray.json._
import spray.json.DefaultJsonProtocol._
import EntitiesJsonProtocol.mapEventFormat
import spray.httpx.SprayJsonSupport._
import ExecutionContext.Implicits.global
import scala.util
import scala.util.{Failure,Success}

class RouteServiceActor(_accountServiceRef: AskableActorRef, _eventService: AskableActorRef) extends Actor with RouteService {
  implicit lazy val timeout = Timeout(10.seconds)
  def actorRefFactory = context
  def accountServiceRef: AskableActorRef = _accountServiceRef
  def eventsServiceRef = _eventService
  def awaitResult(future: Future[Any]): Any = {
    Await.result(future, Duration(2, duration.SECONDS))
  }

  override def sendHello = {
    val future: Future[Any] = accountServiceRef ? RouteHello("Q")
    Await.result(future, Duration(2, duration.SECONDS)) match {
      case AccountHello(msg) => s"msg : $msg"
      case _ => s"unhandled message"
    }
  }
  override def sendIsAuthorized(session: String) = {
    awaitResult(accountServiceRef ? IsAuthorized(session)).asInstanceOf[String]
  }
  override def sendAuthorize(session: String, clientId: String, token: String): String = {"authorize"}
  override def sendGetEvents(): Future[Any] = {
    eventsServiceRef ? EventService.GetEvents()
  }

  def receive = handleMessages orElse runRoute(myRoute)

  //это если актору надо принять наши сообщения
  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)

  }
  override def sendAddEvent(event: MapEvent, user: User): Future[Any] = {
    eventsServiceRef ? EventService.AddEvent(event, user)
  }

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
  def sendIsAuthorized(session: String): String
  def sendAuthorize(session: String, clientId: String, token: String) : String
  //  def sendUnauthorize : String

  //event service send
  def sendAddEvent(event:MapEvent,user:User): Future[Any]
  def sendGetEvents(): Future[Any]



  val auth = pathPrefix("auth") {
    cookie("JSESSIONID") {
      jSession => {
        get {
          complete(sendIsAuthorized(jSession.content))
        } ~
        post {
          parameters('clientId, 'token) {(clientId, token) =>
            complete(sendAuthorize(jSession.content, clientId, token))
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
        } ~
        post{
          entity(as[MapEvent]){ event =>
            val future = sendAddEvent(event,User("fwefwef",entities.db.Roles.USER.getRoleId,Some(1)))
            onComplete(future){
              case Success(item) => {
                println("complete")
                complete(item.asInstanceOf[ResponseEvent].event)
              }
              case util.Failure(t) =>{
                println("fail?")
                complete("fail?")
              }
            }
          }
        }
    } ~
    pathEnd {
      get {
        onComplete(sendGetEvents()){
          case Success(items) => {
            println("got events")
            complete(responseSuccess(Some(items.asInstanceOf[Seq[MapEvent]])).toJson.prettyPrint)
          }
          case Failure(t) => {
            println("failed")
            complete("failed")
          }
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

  val myRoute = {
    auth ~
    other ~
    event
  }
}

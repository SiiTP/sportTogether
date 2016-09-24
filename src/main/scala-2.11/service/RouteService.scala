package service

import akka.actor.Actor
import akka.pattern.AskableActorRef
import akka.util.Timeout
import service.AccountService.{AccountHello}
import service.RouteServiceActor.{IsAuthorized, RouteHello}
import spray.routing._

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}

class RouteServiceActor(_accountServiceRef: AskableActorRef) extends Actor with RouteService {
  implicit lazy val timeout = Timeout(5.seconds)
  def actorRefFactory = context
  def accountServiceRef: AskableActorRef = _accountServiceRef
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
    awaitResult(accountServiceRef ? IsAuthorized(session)) match {

    }
  }

  override def sendAuthorize: String = ???

  override def sendUnauthorize: String = ???



  def receive = handleMessages orElse runRoute(myRoute)

  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)
  }


}

object RouteServiceActor {
  case class RouteHello(msg: String)

  case class Authorize(session: String, token: String, id: Int)
  case class IsAuthorized(session: String)
  case class Unauthorize(session: String)
  case class GetAccount(session: String)
  case class CreateAccount(session: String, token: String, role: Int)
}

trait RouteService extends HttpService {
  def accountServiceRef: AskableActorRef

  def sendHello : String
  def sendIsAuthorized : String
  def sendAuthorize : String
  def sendUnauthorize : String

  val auth = pathPrefix("auth") {
    cookie("JSESSIONID") {
      name => {
        get {
          complete(sendIsAuthorized)
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
    other
  }
}

package service

import akka.actor.Actor
import spray.routing._

class RouteServiceActor extends Actor with RouteService {
  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait RouteService extends HttpService {

  private val PROJECT_DIR = System.getProperty("user.dir")

  val auth = pathPrefix("auth") {
    get {
      complete("auth!")
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
    } ~
      getFromDirectory(PROJECT_DIR + "/frontend/")
  }

  val myRoute = {
    auth ~
    other
  }
}

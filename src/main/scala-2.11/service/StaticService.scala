package service

import akka.actor.Actor
import spray.routing._

class StaticServiceActor extends Actor with StaticService {
  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait StaticService extends HttpService {

  private val PROJECT_DIR = System.getProperty("user.dir")
  private val FRONTEND_PATH = "frontend/"
  private val RESOURCES_PATH = "src/main/resources/"

  val myRoute = {
     get {
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




  }
}

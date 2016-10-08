import java.lang.Throwable
import java.util.concurrent.ExecutionException

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import entities.db.{DatabaseExecutor, DatabaseHelper}
import response.MyResponse
import service._
import spray.can.Http

case class my(msg: String)


import scala.concurrent.duration._

import dispatch._, Defaults._
import scala.util.{Success, Failure}

/**
  * Created by root on 11.07.16.
  */
object App extends MyResponse {
  def main(args: Array[String]): Unit = {
    val logger = Logger("webApp")
    logger.info("Server init begin")
    val params = Map("id_token" -> "some_token")
    val req = url("https://x-devel.auth0.com/tokeninfo/") << params
    val response : Future[String] = dispatch.Http.configure(_ setFollowRedirects true)(req.POST OK as.String)

    val eventualInt: Future[Int] = response map (content => {
      println(content)
      1
    }) recover {
      case exc: Throwable =>
        exc.printStackTrace()
        0
    }
    eventualInt.onSuccess {
      case content => {
        println(content + "!#!")
      }
    }

    var port = 8080
    if(args.length > 0){
        args(0) match {
          case str => port = str.toInt
        }
    }
    logger.info(s"Starting on port $port")
    DatabaseExecutor.config("mysqlDB")
    val dbHelper = DatabaseHelper.getInstance
    dbHelper.init(App.getClass.getResourceAsStream("application.conf"))

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("actor-system-1")
    implicit val timeout = Timeout(5.seconds)

    val fcmService =  system.actorOf(Props(classOf[FcmServiceActor]),"fcmService")
    // create and start our service actor
    val eventService = new EventService()
    val eventServiceActor = system.actorOf(Props(classOf[EventServiceActor],eventService),"eventService")

    val categoryService = new CategoryService()
    val categoryServiceActor = system.actorOf(Props(classOf[CategoryServiceActor],categoryService),"categoryService")

    val accountService = new AccountService()
    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor,eventServiceActor,categoryServiceActor, fcmService), "routeService")

//    var port = 8080
//    args(0) match {
//      case str => port = str.toInt
//    }
//    DatabaseExecutor.config("mysqlDB")
//    val dbHelper = DatabaseHelper.getInstance
//    dbHelper.init(App.getClass.getResourceAsStream("application.conf"))
//
//    // we need an ActorSystem to host our application in
//    implicit val system = ActorSystem("actor-system-1")
//    implicit val timeout = Timeout(5.seconds)
//
//    // create and start our service actor
//    val eventService = new EventService()
//    val eventServiceActor = system.actorOf(Props(classOf[EventServiceActor],eventService),"eventService")
//
//    val categoryService = new CategoryService()
//    val categoryServiceActor = system.actorOf(Props(classOf[CategoryServiceActor],categoryService),"categoryService")
//
//    val accountService = new AccountService()
//    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
//    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor,eventServiceActor,categoryServiceActor), "routeService")
//
//    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(routeServiceActor, interface = "localhost", port = port)
    logger.info("Init complete!")
  }
}

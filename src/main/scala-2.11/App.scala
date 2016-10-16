import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import entities.db.{DatabaseExecutor, DatabaseHelper}
import response.MyResponse
import service._
import spray.can.Http

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

    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor, eventServiceActor, categoryServiceActor, fcmService), "routeService")

    val future: Future[Any] = IO(Http) ? Http.Bind(routeServiceActor, interface = "localhost", port = port)
    future.onComplete {
      case Success(a) =>
        println("@!@! " + a)
      case Failure(e) =>
        e.printStackTrace()
    }
    logger.info("Init complete!")
  }
}

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import entities.{DatabaseHelper, MapCategory, MapEvent}
import service.{AccountService, RouteServiceActor}
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import dao.{AccountDAO, CategoryDAO, EventsDAO}
import service.AccountService.Authorize

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

/**
  * Created by root on 11.07.16.
  */
object App {
  def main(args: Array[String]): Unit = {
    DatabaseHelper.init(App.getClass.getProtectionDomain.getCodeSource.getLocation.getPath)

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("actor-system-1")

    // create and start our service actor
    val routeService = system.actorOf(Props[RouteServiceActor], "route-service")
    val accountService = system.actorOf(Props[AccountService], "account-service")

    accountService ! Authorize("sessionStr", "tokenStr")

    implicit val timeout = Timeout(5.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(routeService, interface = "localhost", port = 8080)


    Thread.sleep(1000)
  }
}

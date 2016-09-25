import java.net.URL

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import entities.db.{DatabaseHelper, EntitiesJsonProtocol, User}
import service.{AccountService, AccountServiceActor, RouteServiceActor}
import spray.can.Http

case class my(msg: String)


import scala.concurrent.duration._

/**
  * Created by root on 11.07.16.
  */
object App {
  def main(args: Array[String]): Unit = {
    import response.ResponseJsonProtocol._
    import EntitiesJsonProtocol._
    import spray.json._
    println(User("clientId", 1, Some(1)).toJson.prettyPrint)
    println(User("clientId", 2, None).toJson.prettyPrint)
    println(User("clientId", 3).toJson.prettyPrint)
    val print1: String = responseError(1, "azaza").toJson.prettyPrint
    DatabaseHelper.config("mysqlDB")
    val dbHelper = new DatabaseHelper()
    dbHelper.init(App.getClass.getResource("application.conf").getPath)

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("actor-system-1")
    implicit val timeout = Timeout(5.seconds)

    // create and start our service actor
    val accountService = new AccountService()
    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor), "routeService")

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(routeServiceActor, interface = "localhost", port = 8080)

  }
}

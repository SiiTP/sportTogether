import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import entities.db.{DatabaseHelper, EntitiesJsonProtocol, User}
import response.MyResponse
import service._
import spray.can.Http

case class my(msg: String)


import scala.concurrent.duration._

/**
  * Created by root on 11.07.16.
  */
object App extends MyResponse {
  def main(args: Array[String]): Unit = {
//    import EntitiesJsonProtocol._
//    import spray.json._
//
//    println(User("a", 1, Some(1)).toJson.prettyPrint)
//    println(responseSuccess[User](None).toJson.prettyPrint)
    DatabaseHelper.config("mysqlDB")
    val dbHelper = new DatabaseHelper()
    dbHelper.init(App.getClass.getResource("application.conf").getPath)

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("actor-system-1")
    implicit val timeout = Timeout(5.seconds)

    // create and start our service actor
    val eventService = new EventService()
    val eventServiceActor = system.actorOf(Props(classOf[EventServiceActor],eventService),"eventService")

    val accountService = new AccountService()
    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor,eventServiceActor), "routeService")

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(routeServiceActor, interface = "localhost", port = 8080)

  }
}

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import entities.db.{DatabaseHelper, DatabaseExecutor}
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
    var port = 8080
    args(0) match {
      case str => port = str.toInt
    }
    DatabaseExecutor.config("mysqlDB")
    val dbHelper = DatabaseHelper.getInstance
    dbHelper.init(App.getClass.getResourceAsStream("application.conf"))

    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("actor-system-1")
    implicit val timeout = Timeout(5.seconds)

    // create and start our service actor
    val eventService = new EventService()
    val eventServiceActor = system.actorOf(Props(classOf[EventServiceActor],eventService),"eventService")

    val categoryService = new CategoryService()
    val categoryServiceActor = system.actorOf(Props(classOf[CategoryServiceActor],categoryService),"categoryService")

    val accountService = new AccountService()
    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor], accountServiceActor,eventServiceActor,categoryServiceActor), "routeService")

    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(routeServiceActor, interface = "localhost", port = port)

  }
}

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import entities.db.{DatabaseExecutor, DatabaseHelper}
import response.MyResponse
import service._
import service.support.RedisConfig
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
    val reminderService = new ReminderService(fcmService)
    reminderService.start()
    val reminderServiceActor = system.actorOf(Props(classOf[ReminderServiceActor],reminderService),"reminderService")
    val messageServiceActor = system.actorOf(Props(classOf[MessageServiceActor], fcmService),"messageService")
    val joinService = new JoinEventService()
    val joinServiceActor = system.actorOf(Props(classOf[JoinEventServiceActor], joinService, messageServiceActor),"joinService")
    messageServiceActor ! MessageService.InitJoinEventService(joinServiceActor)
    // create and start our service actor
    val categoryService = new CategoryService()
    val categoryServiceActor = system.actorOf(Props(classOf[CategoryServiceActor],categoryService),"categoryService")

    val taskService = new TaskService()
    val taskServiceActor = system.actorOf(Props(classOf[TaskServiceActor], taskService), "taskService")
    val eventService = new EventService()
    val eventServiceActor = system.actorOf(Props(classOf[EventServiceActor],eventService, reminderServiceActor, categoryService, messageServiceActor, joinService, taskService),"eventService")
    val redisClientPool = RedisConfig.createConfig("application.conf")
    val accountService = new AccountService(redisClientPool)
    val accountServiceActor = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")
    val routeServiceActor = system.actorOf(Props(classOf[RouteServiceActor],
      accountServiceActor,eventServiceActor,categoryServiceActor,
      fcmService, joinServiceActor, taskServiceActor), "routeService")


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

import java.io.File
import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import dao.UserDAO
import entities.DatabaseHelper
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import service.StaticServiceActor

import scala.io.Source

/**
  * Created by root on 11.07.16.
  */
object App {
  def main(args: Array[String]): Unit = {
//    val file = new File("/home/ivan/new-stakan4ik/frontend/test.txt")
//    val path: String = file.getAbsolutePath
//    val name: String = file.getName
//    val path1: String = file.getPath
//    val path2: String = file.getCanonicalPath
    DatabaseHelper.create()
    val PATH = getClass.getResource("css/test.css").getPath
    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("on-spray-can")

    // create and start our service actor
    val service = system.actorOf(Props[StaticServiceActor], "demo-service")

    implicit val timeout = Timeout(5.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
  }
}

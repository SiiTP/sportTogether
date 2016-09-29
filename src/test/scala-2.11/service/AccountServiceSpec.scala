package service

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import response.AccountResponse
import service.RouteServiceActor.{Authorize, IsAuthorized}
import spray.testkit.Specs2RouteTest

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import akka.util.Timeout
import dao.UserDAO
import entities.db.{DatabaseHelper, EntitiesJsonProtocol, Roles, User}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.specs2.execute.{AsResult, ResultExecution}

import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * Created by root on 28.09.16.
  */
class AccountServiceSpec extends FlatSpec with Matchers with BeforeAndAfter with AccountResponse {
  println("before")
  implicit val system = ActorSystem("actor-system-test")
  implicit val timeout = Timeout(Duration.create(5, SECONDS))

  val configFile = new File(getClass.getResource("../application_test.conf").getPath)
  DatabaseHelper.config("mysqlDB-test", configFile)
  val dbHelper = new DatabaseHelper()
  dbHelper.init(configFile.getPath)
  val userDAO = new UserDAO

  val accountService = new AccountService()
  val accountServiceActor : AskableActorRef = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")


  "The account service" should "answer correctly to not authorized user" in {
      val answer = Await.result(accountServiceActor ? IsAuthorized("1"), timeout.duration).asInstanceOf[String]
      answer contains responseNotAuthorized.toJson.prettyPrint
    }

    it should "correctly answer when authorize unexisted user" in {
      import ExecutionContext.Implicits.global

      val future: Future[String] = (accountServiceActor ? Authorize("1", "clientid", "token")).asInstanceOf[Future[String]]
      future.onComplete({
        case Success(answer) =>
          answer shouldEqual responseNotSuccess().toJson.prettyPrint
        case Failure(e) => 1 + 1 shouldEqual(2)
      })
      Await.result(future, timeout.duration)
      1 + 1 shouldEqual(2)
    }

    it should  "correctly authorize user that exists" in {
      import EntitiesJsonProtocol._
      import ExecutionContext.Implicits.global

      userDAO.create(User("clientid", Roles.USER.getRoleId))
      val future: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
      future.onComplete({
        case Success(answer) =>
          println("in success")
          println(answer)
          val print1: String = responseSuccess[User](None).toJson.prettyPrint
          println(print1)
          answer shouldEqual print1
        case Failure(e) =>
          println("FAIL")
          1 + 1 shouldEqual 2

      })
      Await.result(future, timeout.duration)
      1 + 1 shouldEqual 2
    }

  after {
    println("clear tables")
    dbHelper.clearTables
  }
}

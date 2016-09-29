package service

import java.io.File
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import response.AccountResponse
import service.RouteServiceActor.{Authorize, IsAuthorized, Unauthorize}
import spray.testkit.Specs2RouteTest

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import akka.util.Timeout
import dao.UserDAO
import entities.db.{DatabaseHelper, EntitiesJsonProtocol, Roles, User}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.specs2.execute.{AsResult, ResultExecution}

import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * Created by root on 28.09.16.
  */
class AccountServiceSpec extends FlatSpec with Matchers with BeforeAndAfter with AccountResponse with ScalaFutures {
  println("before")
  implicit val system = ActorSystem("actor-system-test")
  implicit val timeout = Timeout(Duration.create(5, SECONDS))
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))


  val configFile = new File(getClass.getResource("../application_test.conf").getPath)
  DatabaseHelper.config("mysqlDB-test", configFile)
  val dbHelper = new DatabaseHelper()
  dbHelper.init(configFile.getPath)
  val userDAO = new UserDAO

  val accountService = new AccountService()
  val accountServiceActor : AskableActorRef = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")


  "The account service" should "answer correctly to not authorized user" in {
    val authorizedFuture: Future[String] = (accountServiceActor ? IsAuthorized("1")).asInstanceOf[Future[String]]
    whenReady(authorizedFuture) { result =>
      result shouldEqual responseNotAuthorized.toJson.prettyPrint
    }
  }

    it should "correctly answer when authorize unexisted user" in {
      val future: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
      whenReady(future) { result =>
        result shouldEqual responseNotSuccess().toJson.prettyPrint
      }
    }

    it should  "correctly authorize user that exists" in {
      import EntitiesJsonProtocol._

      val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
      whenReady(userCreateFuture) { user =>
        val authFuture: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
        whenReady(authFuture) { result =>
          result shouldEqual responseSuccess[User](None).toJson.prettyPrint
        }
      }
    }

  it should "not authorize user that already authorized" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
      whenReady(authFuture) { result =>
        result shouldEqual responseSuccess[User](None).toJson.prettyPrint
        val authFuture2: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
        whenReady(authFuture2) { result =>
          result shouldEqual responseAlreadyAuthorized.toJson.prettyPrint
        }
      }
    }
  }

  it should "correctly answer when unauthorize not authorized user" in {
    val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("1")).asInstanceOf[Future[String]]
    whenReady(unauthFuture) {unauthResponse =>
      println(responseNotSuccess().toJson.prettyPrint)
      unauthResponse shouldEqual responseNotAuthorized.toJson.prettyPrint
    }
  }

  it should "correctly unauthorize account" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]

      whenReady(authFuture) { result =>
        result shouldEqual responseSuccess[User](None).toJson.prettyPrint
        val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("1")).asInstanceOf[Future[String]]

        whenReady(unauthFuture) {unauthResponse =>
          unauthResponse shouldEqual responseSuccess[User](None).toJson.prettyPrint
        }
      }
    }

  }

  it should "correctly authorize again" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]

      whenReady(authFuture) { result =>
        result shouldEqual responseSuccess[User](None).toJson.prettyPrint
        val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("1")).asInstanceOf[Future[String]]

        whenReady(unauthFuture) {unauthResponse =>
          unauthResponse shouldEqual responseSuccess[User](None).toJson.prettyPrint

          val authFuture: Future[String] = (accountServiceActor ? Authorize("1", "token", "clientid")).asInstanceOf[Future[String]]
          whenReady(authFuture) { authResponse =>
            authResponse shouldEqual responseSuccess[User](None).toJson.prettyPrint
          }
        }
      }
    }
  }

  after {
    println("clear tables")
    dbHelper.clearTables
  }
}

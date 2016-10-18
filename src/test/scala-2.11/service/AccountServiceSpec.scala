package service

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.pattern.AskableActorRef
import response.AccountResponse
import service.RouteServiceActor.{Authorize, IsAuthorized, Unauthorize}

import scala.concurrent.duration._
import akka.util.Timeout
import dao.UserDAO

import entities.db.{DatabaseHelper, EntitiesJsonProtocol, Roles, User}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import entities.db._


import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * Created by root on 28.09.16.
  */

class AccountServiceSpec extends FlatSpec with Matchers with BeforeAndAfter with ScalaFutures {
  implicit val system = ActorSystem("actor-system-test")
  implicit val timeout = Timeout(Duration.create(5, SECONDS))
  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))


  val configFile = new File(getClass.getResource("../application_test.conf").getPath)
  DatabaseExecutor.config("mysqlDB-test", configFile)

  val dbHelper = DatabaseHelper.getInstance
  dbHelper.init(getClass.getResourceAsStream("../application_test.conf"))
  val userDAO = new UserDAO

  val accountService = new AccountService()
  val accountServiceActor : AskableActorRef = system.actorOf(Props(classOf[AccountServiceActor], accountService), "accountService")

  "The account service" should "answer correctly to not authorized user" in {
    val authorizedFuture: Future[String] = (accountServiceActor ? IsAuthorized("1")).asInstanceOf[Future[String]]
    whenReady(authorizedFuture) { result =>
      result shouldEqual AccountResponse.responseNotAuthorized.toJson.prettyPrint
    }
  }

    it should "correctly create user when authorize unexisted user" in {
      import EntitiesJsonProtocol._

      val future: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]
      whenReady(future) { result =>
        result shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
      }
    }

    it should  "correctly authorize user that exists" in {
      import EntitiesJsonProtocol._

      val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
      whenReady(userCreateFuture) { user =>
        val authFuture: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]
        whenReady(authFuture) { result =>
          result shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
        }
      }
    }

  it should "not authorize user that already authorized" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]
      whenReady(authFuture) { result =>
        result shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
        val authFuture2: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]
        whenReady(authFuture2) { result =>
          result shouldEqual AccountResponse.responseAlreadyAuthorized.toJson.prettyPrint
        }
      }
    }
  }

  it should "correctly answer when unauthorize not authorized user" in {
    val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("1")).asInstanceOf[Future[String]]
    whenReady(unauthFuture) {unauthResponse =>
      println(AccountResponse.responseNotSuccess.toJson.prettyPrint)
      unauthResponse shouldEqual AccountResponse.responseNotAuthorized.toJson.prettyPrint
    }
  }

  it should "correctly unauthorize account" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]

      whenReady(authFuture) { result =>
        result shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
        val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("clientid")).asInstanceOf[Future[String]]

        whenReady(unauthFuture) {unauthResponse =>
          unauthResponse shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
        }
      }
    }

  }

  it should "correctly authorize again" in {
    import EntitiesJsonProtocol._

    val userCreateFuture: Future[User] = userDAO.create(User("clientid", Roles.USER.getRoleId))
    whenReady(userCreateFuture) { user =>
      val authFuture: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]

      whenReady(authFuture) { result =>
        result shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
        val unauthFuture: Future[String] = (accountServiceActor ? Unauthorize("clientid")).asInstanceOf[Future[String]]

        whenReady(unauthFuture) {unauthResponse =>
          unauthResponse shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint

          val authFuture: Future[String] = (accountServiceActor ? Authorize("token", "clientid")).asInstanceOf[Future[String]]
          whenReady(authFuture) { authResponse =>
            authResponse shouldEqual AccountResponse.responseSuccess[User](None).toJson.prettyPrint
          }
        }
      }
    }
  }

  after {
    println("clear tables")
    accountService.reset()
    dbHelper.clearTables
  }
}

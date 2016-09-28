//package service
//
//import akka.pattern.AskableActorRef
//import entities.db.{MapEvent, User}
//import org.scalamock.scalatest.MockFactory
//import org.specs2.mutable.Specification
//import spray.testkit.Specs2RouteTest
//import spray.routing.HttpService
//import spray.http.StatusCodes._
//
//import scala.concurrent.Future
//
///**
//  * Created by root on 27.09.16.
//  */
//
//class RouteServiceSpec extends Specification with Specs2RouteTest with RouteService {
//  def actorRefFactory = system // connect the DSL to the test ActorSystem
//
//  override def accountServiceRef: AskableActorRef = ???
//
//  override def sendAuthorize(session: String, clientId: String, token: String): String = ???
//  //event service send
//  override def sendAddEvent(event: MapEvent, user: User): Future[Any] = ???
//  override def sendGetEvents(session: String, someFilter: Int): String = ???
//  override def sendHello: String = ???
//  override def sendIsAuthorized(session: String): Future[Any] = ???
////  val smallRoute =
////    get {
////      pathSingleSlash {
////        complete {
////          <html>
////            <body>
////              <h1>Say hello to <i>spray</i>!</h1>
////            </body>
////          </html>
////        }
////      } ~
////        path("ping") {
////          complete("PONG!")
////        }
////    }
//
//  "The service" should {
//
//    "not authorized user" in {
//
//      Get() ~> myRoute ~> check {
//        responseAs[String] must contain("Say hello")
//      }
//    }
//
////    "return a 'PONG!' response for GET requests to /ping" in {
////      Get("/ping") ~> smallRoute ~> check {
////        responseAs[String] === "PONG!"
////      }
////    }
////
////    "leave GET requests to other paths unhandled" in {
////      Get("/kermit") ~> smallRoute ~> check {
////        handled must beFalse
////      }
////    }
////
////    "return a MethodNotAllowed error for PUT requests to the root path" in {
////      Put() ~> sealRoute(smallRoute) ~> check {
////        status === MethodNotAllowed
////        responseAs[String] === "HTTP method not allowed, supported methods: GET"
////      }
////    }
//  }
//
//
//}
////class RouteServiceSpec extends Specification with Specs2RouteTest with RouteServiceActor {
////
//
////}

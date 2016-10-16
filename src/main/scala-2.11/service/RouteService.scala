package service

import akka.actor._
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import service.RouteServiceActor._
import akka.util.Timeout
import entities.db.{EntitiesJsonProtocol, MapCategory, MapEvent, User}
import entities.db.EntitiesJsonProtocol._
import response.{AccountResponse, EventResponse}
import service.AccountService.AccountHello
import service.RouteServiceActor.{IsAuthorized, RouteHello}
import spray.routing._

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, duration}
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._

import ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class RouteServiceActor(_accountServiceRef: AskableActorRef, _eventService: AskableActorRef,_categoryService: AskableActorRef, _fcmService: AskableActorRef, _joinEventService: AskableActorRef) extends Actor with RouteService {
  implicit lazy val timeouts = Timeout(10.seconds)
  def actorRefFactory = context
  def accountServiceRef: AskableActorRef = _accountServiceRef
  def eventsServiceRef = _eventService

  override def sendHello = {
    val future: Future[Any] = accountServiceRef ? RouteHello("Q")
    Await.result(future, Duration(2, duration.SECONDS)) match {
      case AccountHello(msg) => s"msg : $msg"
      case _ => s"unhandled message"
    }
  }
  override def sendIsAuthorized(clientId: String) = {
    accountServiceRef ? IsAuthorized(clientId)
  }
  override def sendAuthorize(clientId: String, token: String): Future[Any] = {
    accountServiceRef ? Authorize(token, clientId)
  }
  override def sendUnauthorize(session: String): Future[Any] = {
    accountServiceRef ? Unauthorize(session)
  }


  override def sendGetEvents() = eventsServiceRef ? EventService.GetEvents()
  def receive = handleMessages orElse runRoute(myRoute)

  //это если актору надо принять наши сообщения
  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)

  }
  override def sendAddEvent(event: MapEvent, user: User) = eventsServiceRef ? EventService.AddEvent(event, user)
  override def sendGetUserEvents(id:Int) = eventsServiceRef ? EventService.GetUserEvents(id)

  override def sendGetEvent(id: Int) = eventsServiceRef ? EventService.GetEvent(id)

  override def sendCreateCategory(name: String): Future[Any] = _categoryService ? CategoryService.CreateCategory(name)

  override def sendGetCategory(id: Int): Future[Any] = _categoryService ? CategoryService.GetCategory(id)

  override def sendGetCategories(): Future[Any] = _categoryService ? CategoryService.GetCategories()

  override def sendGetEventsDistance(distance: Double, latitude: Double, longtitude: Double): Future[Any] = _eventService ? EventService.GetEventsByDistance(distance, longtitude, latitude)

  override def sendUpdateEvents(event: MapEvent, user: User) = _eventService ? EventService.UpdateEvent(event, user)

  override def sendReportEvent(id: Int, user: User) = _eventService ? EventService.ReportEvent(id, user)
  override def sendGetEventsByCategoryId(id: Int) = _eventService ? EventService.GetEventsByCategoryId(id)
  override def sendGetEventsByCategoryName(name: String) = _categoryService ? CategoryService.GetEventsByCategoryName(name)
  override def sendUserJoinEvent(user: User, eventId: Int, token: String): Future[Any] = _joinEventService ? JoinEventService.AddUserToEvent(eventId, user, token)

  override def testMessageSend(token: String): Future[Any] = _fcmService ? FcmService.SendMessage(Array(token),JsObject("hello" -> JsString("world"), "id" -> JsNumber(5), "bools" -> JsBoolean(true)))

}

object RouteServiceActor {
  case class RouteHello(msg: String)

  case class Authorize(token: String, clientId: String)
  case class IsAuthorized(clientId: String)
  case class Unauthorize(clientId: String)
  case class GetAccount(clientId: String)

  case class InitEventService(service: ActorRef)
}

trait RouteService extends HttpService {
  val logger = Logger("webApp")

  def accountServiceRef: AskableActorRef

  def sendHello: String

  def sendIsAuthorized(session: String): Future[Any]

  def sendAuthorize(clientId: String, token: String): Future[Any]

  def sendUnauthorize(session: String): Future[Any]

  def sendAddEvent(event: MapEvent, user: User): Future[Any]

  def sendGetEvents(): Future[Any]

  def sendGetUserEvents(id: Int): Future[Any]

  def sendGetEventsByCategoryId(id: Int): Future[Any]

  def sendGetEventsDistance(distance: Double, latitude: Double, longtitude: Double): Future[Any]

  def sendGetEvent(id: Int): Future[Any]

  def sendGetEventsByCategoryName(name: String): Future[Any]

  def sendGetCategories(): Future[Any]

  def sendGetCategory(id: Int): Future[Any]

  def sendCreateCategory(name: String): Future[Any]

  def sendReportEvent(id: Int, user: User): Future[Any]

  def sendUpdateEvents(event: MapEvent, user: User): Future[Any]
  def sendUserJoinEvent(user: User, eventId: Int, token: String): Future[Any]

  def getStringResponse(data: Any) = data.asInstanceOf[String]

  //т.к. везде одинаковые ответы(строки), то вынес все в одну функцию
  def defaultResponse(a: Try[Any], logMsg: String): Route = {
    logMsg match {
      case "" => println("empty log")
      case s => logger.info(s)
      case _ =>
    }
    a match {
      case Success(result) =>
        val stringResult = getStringResponse(result)
        logger.debug(stringResult)
        complete(stringResult)
      case Failure(t) =>
        logger.info(t.getMessage)
        complete(t.getMessage)
    }
  }

  def defaultResponse(a: Try[Any]): Route = defaultResponse(a, "")

  def auth(token: String, clientId: String) = pathPrefix("auth") {
    get {
      onComplete(sendIsAuthorized(clientId)) { tryAny =>
        defaultResponse(tryAny, s"GET /auth  clientId: $clientId and token: $token")
      }
    } ~
      delete {
        onComplete(sendUnauthorize(clientId)) { tryAny =>
          defaultResponse(tryAny, s"DELETE /auth  clientId: $clientId and token: $token")
        }
      } ~
      post {
        onComplete(sendAuthorize(clientId, token)) { tryAny =>
          defaultResponse(tryAny, s"POST /auth  clientId: $clientId and token: $token")
        }
      }
  }

  def category(user: User) = pathPrefix("category") {
    pathPrefix(IntNumber) {
      id => {
        path("event") {
          get {
            onComplete(sendGetEventsByCategoryId(id)) {
              logger.info(s"GET category/$id/event")
              defaultResponse
            }
          }
        } ~
          pathEnd {
            get {
              onComplete(sendGetCategory(id)) {
                logger.info(s"GET category/$id")
                defaultResponse
              }
            }
          }
      }
    } ~
      pathPrefix(Segment) {
        segment =>
          get {
            onComplete(sendGetEventsByCategoryName(segment)) {
              logger.info(s"GET category/$segment")
              defaultResponse
            }
          }
      } ~
      pathEnd {
        get {

          onComplete(sendGetCategories()) {
            logger.info(s"GET category/")
            defaultResponse
          }
        } ~
          post {
            entity(as[MapCategory]) {
              category =>

                onComplete(sendCreateCategory(category.name)) {
                  logger.info(s"POST category/ data:$category")
                  defaultResponse
                }
            }
          }  ~ complete("no required parameters")
      }
  }

  def event(user: User) = pathPrefix("event") {
    pathPrefix(IntNumber) {
      id =>
        path("report") {
          get {
            complete("hello world")
          } ~
            post {
              onComplete(sendReportEvent(id, user)) {
                logger.info(s"POST event/$id/report")
                defaultResponse
              }
            }
        } ~
        path("join"){
          get {
            parameter("token".as[String]) {
              token =>
                onComplete(sendUserJoinEvent(user,id,token)) {
                  defaultResponse//"eip4vuQhWQU:APA91bEFEEZKOAUBoKwa3RsjU7oTcKTVbWdZbqZ5JB4d5vjJH7H8kFN3hKWKuOovhShpLVt6asIsiWVZdLZvsDHAraftWgltTNMixG7TmQwphH-vjQ6TVMC-QxZs6FZBM8tCJ7O2Qa8v"
                }
            }
          }
        } ~
        pathEnd {
          get {
            onComplete(sendGetEvent(id)) {
              logger.info(s"GET event/$id ")
              defaultResponse
            }
          } ~
          put {
            entity(as[MapEvent]) { event =>

              onComplete(sendUpdateEvents(event.copy(userId = user.id, id = Some(id)), user)) {
                logger.info(s"PUT event/$id data:$event")
                defaultResponse
              }
            }
          }
        }
    } ~
    pathEnd {
      get {
        onComplete(sendGetEvents()) {
          logger.info(s"GET event/")
          defaultResponse
        }
      } ~
        post {
          entity(as[MapEvent]) { event =>

            onComplete(sendAddEvent(event, user)) {
              logger.info(s"POST event/ data:$event")
              defaultResponse
            }
          } ~ complete(EventResponse.noSomeParameters.toJson.prettyPrint)
        }
    } ~
    pathPrefix("user") {
      get {
        onComplete(sendGetUserEvents(user.id.get)) {
          logger.info("GET event/user")
          defaultResponse
        }
      }
    } ~
    pathPrefix("distance") {
      path(DoubleNumber) {
        distance =>
          parameter("latitude".as[Double], "longtitude".as[Double]) {
            (latitude, longtitude) =>
              get {
                onComplete(sendGetEventsDistance(distance, latitude, longtitude)) {
                  logger.info(s"GET event/distance/$distance Latittude $latitude and $longtitude")
                  defaultResponse
                }
              }
          }
      }
    }
  }

  def testMessageSend(token: String): Future[Any]
  val gcm = path("gcm") {
    get {
      onComplete(testMessageSend("eip4vuQhWQU:APA91bEFEEZKOAUBoKwa3RsjU7oTcKTVbWdZbqZ5JB4d5vjJH7H8kFN3hKWKuOovhShpLVt6asIsiWVZdLZvsDHAraftWgltTNMixG7TmQwphH-vjQ6TVMC-QxZs6FZBM8tCJ7O2Qa8v")) {
        case Success(result) => complete(result.asInstanceOf[String])
      }
    }
  }

  val other = get {
    pathPrefix("hello") {
      path("world") {
        complete("Hello world!")
      } ~
      path("token") {
        (headerValueByName("Token") & headerValueByName("ClientId123")) { (t, c) =>
          complete(s"t: $t c: $c")
        } ~
        complete(StatusCodes.Unauthorized, "No some headers!@!@")
      }
    }
  }
  def getRoute = myRoute
  def sessionRequiredRoutes(token: String, clientId: String) = {
    onSuccess(sendIsAuthorized(clientId)){
      case result =>
        logger.info("Is Authorized " + result)
        JsonParser(result.asInstanceOf[String]).convertTo[AccountResponse.ResponseSuccess[User]].data match {
          case Some(u) =>
            logger.debug(s"USER: $u")
            event(u) ~
            category(u)
          case None => {
            logger.info(s"No auth for $clientId")
            complete(AccountResponse.responseNotAuthorized.toJson.prettyPrint)
          }
        }
    }
  }
  val authRoutes = (headerValueByName("Token") & headerValueByName("ClientId")) { (token, clientId) =>
    logger.info("Auth with ClientId " + clientId + " token " + token)
    auth(token, clientId) ~
    sessionRequiredRoutes(token, clientId)
  } ~
  complete(StatusCodes.Unauthorized, "No some headers")

  val myRoute = {
    gcm ~
    other ~
    authRoutes
  }
}


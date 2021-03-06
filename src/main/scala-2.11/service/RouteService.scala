package service

import akka.actor._
import akka.pattern.AskableActorRef
import com.typesafe.scalalogging.Logger
import dao.filters.{CategoryFilters, EventFilters}
import service.RouteServiceActor._
import akka.util.Timeout
import entities.db._
import entities.db.EntitiesJsonProtocol._
import response.{AccountResponse, CategoryResponse, EventResponse, MyResponse}
import service.AccountService.AccountHello
import service.RouteServiceActor.{IsAuthorized, RouteHello}
import spray.routing._
import spray.routing.directives.OnCompleteFutureMagnet

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, duration}
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._

import ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class RouteServiceActor(_accountServiceRef: AskableActorRef,
                        _eventService: AskableActorRef,
                        _categoryService: AskableActorRef,
                        _fcmService: AskableActorRef,
                        _joinEventService: AskableActorRef,
                       _taskServiceActor: AskableActorRef) extends Actor with RouteService {
  implicit lazy val timeouts = Timeout(15.seconds)
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
  override def sendAuthorize(user: User, token: String): Future[Any] = {
    accountServiceRef ? Authorize(user, token)
  }
  override def sendUnauthorize(session: String): Future[Any] = {
    accountServiceRef ? Unauthorize(session)
  }
  override def sendUpdateUser(user: User): Future[Any] = {
    accountServiceRef ? AccountService.UpdateUserInfo(user)
  }

  override def sendGetEvents(param: Map[String,List[String]],user: User) = {

    eventsServiceRef ? EventService.GetEvents(new EventFilters(param),user)
  }
  def receive = handleMessages orElse runRoute(myRoute)

  //это если актору надо принять наши сообщения
  def handleMessages: Receive = {
    case AccountHello(msg) => println("hello from account : " + msg)

  }
  override def sendAddEvent(event: MapEventAdapter, user: User) = {
            eventsServiceRef ? EventService.AddEvent(event,user)

  }
  override def sendGetUserEvents(id:Int,user: User,param: Map[String,List[String]]) = eventsServiceRef ? EventService.GetUserEvents(id, user, new EventFilters(param))

  override def sendGetEvent(id: Int,user: User) = eventsServiceRef ? EventService.GetEvent(id,user)

  override def sendCreateCategory(name: String): Future[Any] = _categoryService ? CategoryService.CreateCategory(name)

  override def sendGetCategory(id: Int): Future[Any] = _categoryService ? CategoryService.GetCategory(id)

  override def sendGetCategories(param: Map[String,List[String]]): Future[Any] = _categoryService ? CategoryService.GetCategories(new CategoryFilters(param))
  override def sendGetCategoriesByPartOfName(subname: String): Future[Any] = _categoryService ? CategoryService.GetCategoriesByPartOfName(subname)


  override def sendGetEventsDistance(distance: Double, latitude: Double, longtitude: Double,
                                     param: Map[String,List[String]],user: User): Future[Any] = {
    _eventService ? EventService.GetEventsByDistance(distance, longtitude, latitude, new EventFilters(param), user)
  }
  override def sendGetSiteEventsDistance(distance: Double, latitude: Double, longtitude: Double,
                                         param: Map[String,List[String]]): Future[Any] = {
    _eventService ? EventService.GetEventsByDistanceSite(distance, longtitude, latitude, new EventFilters(param))
  }
  override def sendUpdateEvents(event: MapEventAdapter, user: User) = _eventService ? EventService.UpdateEvent(event, user)
  override def sendUpdateResult(event: MapEventResultAdapter, user: User) = _eventService ? EventService.UpdateEventResult(event, user)
  override def sendFinishEvent(id: Int, user: User): Future[Any] = _eventService ? EventService.FinishEvent(id, user)
  override def sendReportEvent(id: Int, user: User) = _eventService ? EventService.ReportEvent(id, user)
  override def sendGetEventsByCategoryId(id: Int) = _eventService ? EventService.GetEventsByCategoryId(id)
  override def sendGetEventsByCategoryName(name: String) = _categoryService ? CategoryService.GetEventsByCategoryName(name)
  override def sendGetEventJoinedUsers(eventId: Int): Future[Any] = _joinEventService ? JoinEventService.GetEventJoinedUsers(eventId)

  override def sendUserJoinEvent(user: User, eventId: Int, token: String): Future[Any] = _joinEventService ? JoinEventService.AddUserToEvent(eventId, user, token)
  override def sendGetJoinedEvents(user: User, param: Map[String,List[String]]) = _joinEventService ? JoinEventService.GetEventsOfUserJoined(user, new EventFilters(param))
  override def sendUserLeaveEvent(user: User, eventId: Int): Future[Any] = _joinEventService ? JoinEventService.LeaveEvent(eventId, user)

  override def testMessageSend(token: String): Future[Any] = _fcmService ? FcmService.SendMessage(Array(token),JsObject("hello" -> JsString("world"), "id" -> JsNumber(5), "bools" -> JsBoolean(true)))

  override def sendGetEventTasks(eventId: Int): Future[Any] = _taskServiceActor ? TaskService.GetEventTasks(eventId)
  override def sendUpdateTask(task: EventTask, user: User): Future[Any] = _taskServiceActor ? TaskService.UpdateTasks(task, user)

  override def sendGetTemplates(user: User): Future[Any] = _eventService ? EventService.GetUserTemplates(user)

  override def sendUpdateTemplates(event: MapEventAdapter, user: User): Future[Any] = _eventService ? EventService.UpdateTemplate(event, user)

  override def sendDeleteTemplate(id: Int, user: User): Future[Any] = _eventService ? EventService.DeleteTemplate(id, user)

  override def sendAddTemplates(event: MapEventAdapter, user: User): Future[Any] = _eventService ? EventService.AddTemplate(event, user)
}

object RouteServiceActor {
  case class RouteHello(msg: String)

  case class Authorize(user: User, token: String)
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

  def sendAuthorize(user: User, token: String): Future[Any]

  def sendUnauthorize(session: String): Future[Any]
  def sendUpdateUser(user: User): Future[Any]

  def sendAddEvent(event: MapEventAdapter, user: User): Future[Any]

  def sendGetEvents(param: Map[String,List[String]], user: User): Future[Any]

  def sendGetUserEvents(id: Int, user: User, param: Map[String,List[String]]): Future[Any]
  def sendGetJoinedEvents(user: User, param: Map[String,List[String]]): Future[Any]

  def sendGetEventsByCategoryId(id: Int): Future[Any]

  def sendGetEventsDistance(distance: Double, latitude: Double, longtitude: Double, param: Map[String,List[String]], user: User): Future[Any]
  def sendGetSiteEventsDistance(distance: Double, latitude: Double, longtitude: Double, param: Map[String,List[String]]): Future[Any]

  def sendGetEvent(id: Int, user: User): Future[Any]

  def sendGetEventsByCategoryName(name: String): Future[Any]

  def sendGetCategories(param: Map[String,List[String]]): Future[Any]
  def sendGetCategoriesByPartOfName(subname: String): Future[Any]

  def sendGetCategory(id: Int): Future[Any]

  def sendCreateCategory(name: String): Future[Any]

  def sendReportEvent(id: Int, user: User): Future[Any]
  def sendFinishEvent(id: Int, user: User): Future[Any]
  def sendUpdateEvents(event: MapEventAdapter, user: User): Future[Any]
  def sendUpdateResult(event: MapEventResultAdapter, user: User): Future[Any]
  def sendUserJoinEvent(user: User, eventId: Int, token: String): Future[Any]
  def sendUserLeaveEvent(user: User, eventId: Int): Future[Any]
  def sendGetEventTasks(eventId: Int): Future[Any]
  def sendGetEventJoinedUsers(eventId: Int): Future[Any]
  def sendUpdateTask(task: EventTask, user: User): Future[Any]

  def sendGetTemplates(user: User): Future[Any]
  def sendUpdateTemplates(event: MapEventAdapter, user: User): Future[Any]
  def sendAddTemplates(event: MapEventAdapter,user: User): Future[Any]
  def sendDeleteTemplate(id: Int, user: User): Future[Any]

  def getStringResponse(data: Any) = data.asInstanceOf[String]

  //т.к. везде одинаковые ответы(строки), то вынес все в одну функцию
  def defaultResponse(a: Try[Any], logMsg: String): Route = {
    logMsg match {
      case "" => println("empty log")
      case s => logger.info(s)
    }
    a match {
      case Success(result) =>
        val stringResult = getStringResponse(result)
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
      entity(as[UserAdapter]) { adapter =>
        onComplete(sendAuthorize(User(clientId = Some(clientId),1,name = adapter.name, avatar = adapter.avatar), token)) { tryAny =>

          defaultResponse(tryAny, s"POST /auth  clientId: $clientId and token: $token")
        }
      }
    }
  }

  def category(user: User, params: Map[String,List[String]]) = pathPrefix("category") {
    (pathEnd & parameter("subname".as[String])) { subname =>
      onComplete(sendGetCategoriesByPartOfName(subname)) { tryAny =>
        defaultResponse(tryAny, s"GET /category?subname=" + subname)
      }
    } ~
    pathPrefix(IntNumber) {
      id => {
          pathEnd {
            get {
              onComplete(sendGetCategory(id)) { tryAny =>
                defaultResponse(tryAny, s"GET category/$id")
              }
            }
          }
      }
    } ~
      pathEnd {
        get {

          onComplete(sendGetCategories(params)) { tryAny =>
            defaultResponse(tryAny, s"GET category/")
          }
        } ~
          post {
            entity(as[MapCategory]) {
              category =>

                onComplete(sendCreateCategory(category.name)) { tryAny =>
                  defaultResponse(tryAny, s"POST category/ data:$category")
                }
            }
          } ~ complete(CategoryResponse.unexpectedPath.toJson.prettyPrint)
      } ~ complete(CategoryResponse.unexpectedPath.toJson.prettyPrint)
  }



  def event(user: User, params: Map[String,List[String]]) = pathPrefix("event") {
    pathPrefix(IntNumber) {
      id =>
        path("report") {
          get {
            complete("hello world")
          } ~
            post {
              onComplete(sendReportEvent(id, user)) { tryAny =>
                defaultResponse(tryAny, s"POST event/$id/report")
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
          } ~
          delete {
            onComplete(sendUserLeaveEvent(user, id)) {
              defaultResponse
            }
          }
        } ~
        path("users"){
          get {
            onComplete(sendGetEventJoinedUsers(id)) { tryAny =>
              defaultResponse(tryAny, s"GET event/$id/users")//"eip4vuQhWQU:APA91bEFEEZKOAUBoKwa3RsjU7oTcKTVbWdZbqZ5JB4d5vjJH7H8kFN3hKWKuOovhShpLVt6asIsiWVZdLZvsDHAraftWgltTNMixG7TmQwphH-vjQ6TVMC-QxZs6FZBM8tCJ7O2Qa8v"
            }
          }
        } ~
        pathEnd {
          get {
            onComplete(sendGetEvent(id, user)) { tryAny =>
                defaultResponse(tryAny, s"GET event/$id")
            }
          } ~
          put {
            entity(as[MapEventAdapter]) { event =>
              onComplete(sendUpdateEvents(event.copy(id = Some(id)), user)) { tryAny =>
                defaultResponse(tryAny, s"PUT event/$id data:$event")
              }
            }
          } ~
          delete {
            onComplete(sendFinishEvent(id, user))  { tryAny =>
              defaultResponse(tryAny, s"DELETE event/$id")
            }
          }
        }
    } ~
      (path("joined") & get) {
      onComplete(sendGetJoinedEvents(user, params)) { (tryAny: Try[Any]) =>
        defaultResponse(tryAny, "/event/joined with user : " + user)
      }
    } ~
    (path("result") & put) {
      entity(as[MapEventResultAdapter]) { event =>
        onComplete(sendUpdateResult(event, user)) { (tryAny: Try[Any]) =>
          defaultResponse(tryAny, "/event/result with result adapter : " + event)
        }
      } ~ complete(EventResponse.noSomeParameters.toJson.prettyPrint)
    } ~
    pathEnd {
      get {
        onComplete(sendGetEvents(params, user)) { tryAny =>
          defaultResponse(tryAny, s"GET event/")
        }
      } ~
        post {
          entity(as[MapEventAdapter]) { event =>
            onComplete(sendAddEvent(event, user)) { tryAny =>
              defaultResponse(tryAny, s"POST event/ data:$event")
            }
          } ~ complete(EventResponse.noSomeParameters.toJson.prettyPrint)
        }
    } ~
    pathPrefix("user") {
      get {
        onComplete(sendGetUserEvents(user.id.get, user,params)) { tryAny =>
          defaultResponse(tryAny, "GET event/user")
        }
      }
    } ~
    pathPrefix("distance") {
      path(DoubleNumber) {
        distance =>
          parameter("latitude".as[Double], "longtitude".as[Double]) {
            (latitude, longtitude) =>
              get {
                onComplete(sendGetEventsDistance(distance, latitude, longtitude, params, user)) { tryAny =>
                  defaultResponse(tryAny, s"GET event/distance/$distance Latittude $latitude and $longtitude")
                }
              }
          }
      }
    } ~ complete(EventResponse.unexpectedPath.toJson.prettyPrint)
  }
  def templates(user: User, params: Map[String,List[String]]) = pathPrefix("templates") {
    pathPrefix(IntNumber) {
      id =>
          pathEnd {
              put {
                entity(as[MapEventAdapter]) { event =>
                  onComplete(sendUpdateTemplates(event.copy(id = Some(id)), user)) { tryAny =>
                    defaultResponse(tryAny, s"PUT event/$id data:$event")
                  }
                }
              } ~
              delete {
                onComplete(sendDeleteTemplate(id, user))  { tryAny =>
                  defaultResponse(tryAny, s"DELETE event/$id")
                }
              }
          }
    } ~
      pathEnd {
        get {
          onComplete(sendGetTemplates(user)) { tryAny =>
            defaultResponse(tryAny, s"GET event/")
          }
        } ~
        post {
          entity(as[MapEventAdapter]) { event =>
            onComplete(sendAddTemplates(event, user)) { tryAny =>
              defaultResponse(tryAny, s"POST event/ data:$event")
            }
          } ~ complete(EventResponse.noSomeParameters.toJson.prettyPrint)
        }
      } ~ complete(EventResponse.unexpectedPath.toJson.prettyPrint)
  }
  def task(user: User) = pathPrefix("task") {
    get {
      parameter("eventId".as[Int]) { id =>
        onComplete(sendGetEventTasks(id)) { tryAny =>
          defaultResponse(tryAny, s"GET tasks for id $id")
        }
      }
    } ~
    put {
      entity(as[EventTask]) { task =>
        onComplete(sendUpdateTask(task, user)) { any =>
          defaultResponse(any,s"PUT task with $task")
        }
      }
    }
  }
  def user(user: User) = pathPrefix("user"){
    put {
      entity(as[UserAdapter]) { adapter =>
        onComplete(sendUpdateUser(user.copy(remindTime = adapter.remindTime))) { tryAny =>
          defaultResponse(tryAny, s"PUT /user  clientId: ${user.clientId}")
        }
      }
    }
  }
  def testMessageSend(token: String): Future[Any]

  def eventsNoAuth(params: Map[String,List[String]]) = pathPrefix("event") {
    pathPrefix(DoubleNumber) {
      distance =>
        parameter("latitude".as[Double], "longtitude".as[Double]) {
          (latitude, longtitude) =>
            get {
              onComplete(sendGetSiteEventsDistance(distance, latitude, longtitude, params)) { tryAny =>
                defaultResponse(tryAny, s"SITE GET event/distance/$distance Latittude $latitude and $longtitude")
              }
            }
        }
    }
  }
  val site = (pathPrefix("landing") & parameterMultiMap) {
    params =>
      eventsNoAuth(params)
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
  def sessionRequiredRoutes(token: String, clientId: String, params: Map[String, List[String]]) = {
    onSuccess(sendIsAuthorized(clientId)){
      case result =>
        logger.info("Is Authorized " + result)
        JsonParser(result.asInstanceOf[String]).convertTo[AccountResponse.ResponseSuccess[User]].data match {
          case Some(u) =>
            logger.debug(s"USER: $u")
            event(u,params) ~
            templates(u, params) ~
            category(u, params) ~
            task(u) ~
            user(u)
          case None =>
            logger.info(s"No auth for $clientId")
            complete(AccountResponse.responseNotAuthorized.toJson.prettyPrint)
        }
    }
  }
  val authRoutes = (headerValueByName("Token") & headerValueByName("ClientId") & parameterMultiMap) { (token, clientId,params) =>
    logger.info("Auth with ClientId " + clientId + " token " + token)
    logger.info("Params " + params)
    auth(token, clientId) ~
    sessionRequiredRoutes(token, clientId, params)
  } ~
  complete(StatusCodes.Unauthorized, "No some headers")

  val myRoute = {
    site ~
    other ~
    authRoutes
  }
}


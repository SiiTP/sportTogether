package service

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import dao._
import dao.filters.{CategoryFilters, EventFilters}
import entities.db._
import messages.{FcmMessage, FcmTextMessage}
import response.EventResponse
import service.EventService._
import spray.json._
import entities.db.EntitiesJsonProtocol._;
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
/**
  * Created by ivan on 25.09.16.
  */
class EventService {
  val logger = Logger("webApp")
  private val eventsDAO = new EventsDAO()

  def updateEvent(event: MapEvent, user: User): Future[Int] = {
    getUserEvents(user).flatMap { events =>
      events.map(_.id).contains(event.id) match {
        case true => eventsDAO.update(event)
        case false => Future.successful(0)
      }
    }
  }

  def updateResult(event: MapEventResultAdapter, mapEvent: MapEvent): Future[Int] = {
    val updatedEvent = mapEvent.copy(result = event.result, isEnded = true)
//    logger.info("updated event : " + updatedEvent)
    eventsDAO.update(updatedEvent)
  }

  def reportEvent(id: Int, user: User) = {
    eventsDAO.reportEvent(id, user)
  }
  def addSimpleEvent(event: MapEvent, user: User) = {
    eventsDAO.create(event.copy(userId = user.id))
  }
  def getUserEvents(user: User) = {
    eventsDAO.eventsByUserId(user.id.getOrElse(0))
  }
  def getEventsAround(filters: EventFilters) = {
    eventsDAO.getEvents(filters)
  }
  def finishEvent(id: Int, user: User): Future[MapEvent] = {
    eventsDAO.get(id).andThen({
      case Success(event) =>
        eventsDAO.endEvent(id, user.id.getOrElse(0))
        Future(event)
      case Failure(e) =>
        logger.debug("exception", e)
        Future.failed(e)
    })
  }
  def getUserEvents(id: Int) = eventsDAO.eventsByUserId(id)
  def getEvent(id: Int) = eventsDAO.get(id)
  def getEventsInDistance(distance: Double, lon: Double, lat: Double, filters: EventFilters) = eventsDAO.getNearestEventsByDistance(distance,lon,lat, filters)
}

object EventService {
  case class AddEvent(event: MapEventAdapter,user: User)
  case class GetEvents(filters: EventFilters,user: User)
  case class GetUserEvents(id: Int,user: User)
  case class GetEvent(id: Int,user: User)
  case class GetEventsByDistance(distance: Double, longtitude: Double, latitude: Double, filters: EventFilters,user: User)
  case class UpdateEvent(event: MapEvent, user: User)
  case class ReportEvent(id: Int, user: User)
  case class GetEventsByCategoryId(id: Int)
  case class FinishEvent(id: Int, user: User)
  case class UpdateEventResult(event: MapEventResultAdapter, user: User)

  val logger = Logger("webApp")

  private val eventsDAO = new EventsDAO()
  private val eventUsersDAO = new EventUsersDAO()
  private val categoryDAO = new CategoryDAO()
  private val tasksDao = new TaskDao()
  private val userDao = new UserDAO()

  def toAdapterForm(futureSeq: Future[Seq[MapEvent]], user: User): Future[Seq[MapEventAdapter]] = {
    val futureAdapters = futureSeq.flatMap(seq => {
      eventUsersDAO.getEventsOfUserJoined(user)
        .zip(eventsDAO.getUserReportsEventsId(user.id))
        .zip(categoryDAO.getCategoriesByIds(seq.map(_.categoryId).distinct))
        .zip(userDao.getUsersByIds(seq.map(_.userId.getOrElse(0)).distinct))
        .flatMap(
        (s:(((Seq[MapEvent],Seq[Int]), Seq[MapCategory]),Seq[User])) => {
          Future {
            seq.map(mapEvent => {
              val isJoined = s._1._1._1.exists(_.id == mapEvent.id)
              val isReported = s._1._1._2.contains(mapEvent.id.get)
              val category = s._1._2.find(_.id.get == mapEvent.categoryId)
              val user = s._2.find(_.id == mapEvent.userId)
              MapEventAdapter(
                mapEvent.name,
                category.getOrElse(MapCategory("")),
                mapEvent.latitude,
                mapEvent.longtitude,
                mapEvent.date,
                mapEvent.currentUsers,
                mapEvent.maxPeople,
                mapEvent.reports,
                mapEvent.description,
                mapEvent.result,
                None,
                mapEvent.isEnded,
                isJoined,
                isReported,
                user.map(u=> u.copy(clientId = None)),
                mapEvent.id
              )
            })
          }
        })
      })
    futureAdapters.recoverWith {
      case e: Throwable =>
        logger.error("in to adapter function : " + e.getMessage)
        Future.successful(Seq.empty[MapEventAdapter])
    }
  }
}
class EventServiceActor(eventService: EventService,
                        remingderServiceActor: ActorRef,
                        categoyService: CategoryService,
                        messageServiceActor: ActorRef,
                        joinEventService: JoinEventService,
                        taskService: TaskService) extends Actor {
  implicit lazy val timeouts = Timeout(15.seconds)


  override def receive = {
    case AddEvent(event,user) =>
      val sended = sender()
      categoyService.getAllCategories(new CategoryFilters(Map("category:name"-> List(event.category.name)))).onSuccess {
        case categories =>
//          logger.debug("found categories match names " + categories)
          var response: Future[MapEvent] = null
          if (categories.nonEmpty) {
            response = eventService.addSimpleEvent(event.copy(category = categories.head).toMapEvent,user)
          } else {
            response = categoyService.createCategory(event.category.name).flatMap((category: MapCategory) => {
              logger.debug("created new category: " + category.name)
              logger.debug("add new event: " + event.name)
              eventService.addSimpleEvent(event.copy(category = category).toMapEvent,user)
            })
          }

          new EventsServiceFetcher(user).fetchOne(response).onComplete {
            case Success(result) =>
              val tasks = event.tasks.getOrElse(Seq())
              if (tasks.nonEmpty) {
                taskService.createTasks(tasks.map(task => task.copy(eventId = result.id))).onComplete{
                  case Success(createdTasks) =>
                    sended ! EventResponse.responseSuccess(Some(result.copy(tasks = Some(createdTasks)))).toJson.prettyPrint
                    remingderServiceActor ! ReminderService.Add(result.toMapEvent)
                  case Failure(t) =>
                    logger.debug("exception add tasks", t)
                    sended ! EventResponse.unexpectedError(t.getMessage).toJson.prettyPrint
                }
              } else {
                sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
                remingderServiceActor ! ReminderService.Add(result.toMapEvent)
              }
            case Failure(t) =>
              logger.debug("exception add event", t)
              sended ! EventResponse.unexpectedError(t.getMessage).toJson.prettyPrint
          }
      }
    case GetEvents(filters, user) =>
      val sended = sender()
      new EventsServiceFetcher(user).fetch(eventService.getEventsAround(filters)).onComplete {
        case Success(result) =>
          logger.info(s"success when get events : " + result.length)
          sended ! EventResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(e) =>
          logger.info(s"fail when get events : " + e.getMessage)
          sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
      }
    case GetUserEvents(id, user) =>
      val sended = sender()
      new EventsServiceFetcher(user).fetch(eventService.getUserEvents(id)).onSuccess {
        case eventsSeq => sended ! EventResponse.responseSuccess(Some(eventsSeq)).toJson.prettyPrint
      }
    case GetEvent(id, user) =>
      val sended = sender()
      val eventFuture = eventService.getEvent(id)
      new EventsServiceFetcher(user).fetchOne(eventFuture).onComplete {
        case Success(event) => sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.notFoundError.toJson.prettyPrint
      }
    case GetEventsByDistance(distance, longtitude, latitude, filters, user) =>
      val sended = sender()
      new EventsServiceFetcher(user).fetch(eventService.getEventsInDistance(distance, longtitude, latitude, filters)).onComplete {
        case Success(events) =>
          logger.debug("got events: " + events.size)
          sended ! EventResponse.responseSuccess(Some(events)).toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }

    case UpdateEvent(event, user) =>

      val sended = sender()
      eventService.updateEvent(event,user).onComplete {
        case Success(updatedEvent) if updatedEvent > 0 =>
          sended ! EventResponse.responseSuccess(Some(event)).toJson.prettyPrint
        case Success(updatedEvent) if updatedEvent == 0 =>
          sended ! EventResponse.unexpectedError.toJson.prettyPrint
        case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
      }

    case UpdateEventResult(event, user) =>
//      logger.debug("updating result " + event + " \nUSER " + user)

      val sended = sender()
      eventService.getEvent(event.id).onComplete {
        case Success(mapEvent) =>
          if (mapEvent.userId == user.id && !mapEvent.isEnded) {
            eventService.updateResult(event, mapEvent).onComplete {
              case Success(updatedEvent) if updatedEvent > 0 =>
                sended ! EventResponse.responseSuccess[MapEvent](None).toJson.prettyPrint
                messageServiceActor ! MessageService.SendEventsTextMessage(event.id,
                  FcmTextMessage( event.result.getOrElse(""),"Итог события", FcmMessage.RESULT, Some(mapEvent.toJson)))
              case Success(updatedEvent) if updatedEvent == 0 =>
                sended ! EventResponse.notFoundError.toJson.prettyPrint
              case Failure(t) => sended ! EventResponse.unexpectedError.toJson.prettyPrint
            }
          } else {
            sended ! EventResponse.alreadyPostedResult.toJson.prettyPrint
          }
        case Failure(t) =>
          logger.debug("exception get event", t)
          sended ! EventResponse.notFoundError.toJson.prettyPrint
      }

    case ReportEvent(id, user) =>
      val sended = sender()
      eventService.reportEvent(id, user).onComplete {
        case Success(result) => sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
        case Failure(t) =>
          logger.debug("exception ", t)
          sended ! EventResponse.alreadyReport.toJson.prettyPrint
      }
    case FinishEvent(id, user) =>
      val sended = sender()
      joinEventService.getTokens(id).onComplete {
        case Success(tokens) =>
          eventService.finishEvent(id, user).onComplete {
            case Success(result) =>
              sended ! EventResponse.responseSuccess(Some(UserReport(user.id.get,id))).toJson.prettyPrint
              messageServiceActor ! MessageService.SendTokensTextMessage(tokens.map(_.deviceToken),FcmTextMessage(result.name,"Событие отменено", FcmMessage.CANCELLED))
            case Failure(t) =>
              logger.info("join event failure : ", t)
              sended ! EventResponse.alreadyReport.toJson.prettyPrint
          }
        case Failure(t) =>
          logger.debug("exception ", t)
          sended ! EventResponse.unexpectedError(t.getMessage).toJson.prettyPrint
      }

  }
}

class EventsServiceFetcher(user: User) {
  val categoryDAO = new CategoryDAO()
  val eventsDAO = new EventsDAO()
  val taskDao = new TaskDao()
  val logger = Logger("webApp")
  def fetch(eventsFuture: Future[Seq[MapEvent]]): Future[Seq[MapEventAdapter]] = {
    EventService.toAdapterForm(eventsFuture, user: User)
  }
  def fetchOne(f: Future[MapEvent]): Future[MapEventAdapter] = {
    f.flatMap((f:MapEvent) => {
      EventService.toAdapterForm(Future{Seq(f)}, user: User).map((f:Seq[MapEventAdapter]) => f.head)
    })
  }
}
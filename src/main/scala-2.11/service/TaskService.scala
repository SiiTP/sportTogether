package service

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.typesafe.scalalogging.Logger
import dao.{UserDAO, EventsDAO, TaskDao}
import entities.db.{EventTaskAdapter, User, EventTask}
import response.{TaskResponse, MyResponse}
import service.TaskService.{UpdateTasks, GetEventTasks, CreateTasks}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import entities.db.EntitiesJsonProtocol._

class TaskService {
  private val taskDao = new TaskDao()
  private val eventDao = new EventsDAO()
  private val logger = Logger("webApp")
  def createTasks(tasks: Seq[EventTask]) = {
    taskDao.createTasks(tasks)
  }
  def getEventTasks(eId: Int) = {
    taskDao.getEventTasks(eId)
  }
  def updateTask(task: EventTask, user: User): Future[Option[EventTask]] = {
    taskDao.get(task.id.getOrElse(0)).flatMap(oldTask => {
      if (oldTask.eventId == task.eventId) {
        logger.debug(s"update: found old $oldTask")
        eventDao.get(oldTask.eventId.getOrElse(0)).flatMap(event => {
          logger.debug(s"update: found event $event")
          val isMessageEquals = oldTask.message.equals(task.message)
          if (!isMessageEquals && user.id == event.userId) {
            oldTask.userId match {
              case Some(id) =>
                Future{None}
              case None =>
                val newTask = task.copy(eventId = event.id)
                taskDao.update(newTask).map(count => Some(newTask))
            }
          } else {
            val newTask = task.copy(message = oldTask.message, userId = user.id, eventId = event.id)
            oldTask.userId match {
              case None =>
                taskDao.update(newTask).map(count => Some(newTask))
              case Some(id) =>
                if (id == user.id.get) {
                  taskDao.update(newTask.copy(userId = None)).map(count => Some(newTask.copy(userId = None)))
                } else {
                  Future{None}
                }
            }
          }
        })
      } else {
        Future.successful(None)
      }
    })

  }
}

object TaskService {
  case class CreateTasks(tasks: Seq[EventTask])
  case class GetEventTasks(eId: Int)
  case class UpdateTasks(task: EventTask, user: User)
}

class TaskServiceActor(taskService: TaskService) extends Actor {
  private val logger = Logger("webApp")
  private val joinService = new JoinEventService()
  override def receive: Receive = {
    case CreateTasks(tasks) =>
      val sended = sender()
      taskService.createTasks(tasks).onComplete {
        case Success(result) =>
//          logger.debug(s"added tasks $result")
          sended ! TaskResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(t) =>
          logger.debug("exception add tasks", t)
          sended ! TaskResponse.unexpectedError(t.getMessage).toJson.prettyPrint

      }
    case GetEventTasks(eId) =>
      val sended = sender()
      val taskAdapter = new TaskAdapter()
      logger.debug(s"tasks for id $eId")
      taskAdapter.toTaskAdapter(taskService.getEventTasks(eId)).onComplete {
        case Success(tasks) =>
          logger.debug(s"got tasks $tasks")
          sended ! TaskResponse.responseSuccess(Some(tasks)).toJson.prettyPrint
        case Failure(t) =>
          logger.debug("exception get tasks", t)
          sended ! TaskResponse.unexpectedError(t.getMessage).toJson.prettyPrint

      }
    case UpdateTasks(task, user) =>
      val sended = sender()
      joinService.isUserAlreadyJoined(user, task.eventId.getOrElse(0)).onComplete {
        case Success(isAlreadyJoined) =>
          if(isAlreadyJoined) {
            taskService.updateTask(task, user).onComplete {
              case Success(result) =>
                result match {
                  case some: Some[EventTask] =>
                    //              logger.debug(s"updated tasks $some")
                    sended ! TaskResponse.responseSuccess(some).toJson.prettyPrint
                  case None =>
                    //              logger.debug(s"cant update tasks $task")
                    sended ! TaskResponse.somebodyAccepted.toJson.prettyPrint
                }

              case Failure(t) =>
                logger.debug("exception update tasks", t)
                sended ! TaskResponse.unexpectedError(t.getMessage).toJson.prettyPrint

            }
          } else {
            sended ! TaskResponse.mustBeJoinedToEvent.toJson.prettyPrint
          }
        case Failure(t) =>
          logger.debug("exception is joined", t)
          sended ! TaskResponse.unexpectedError(t.getMessage).toJson.prettyPrint
      }


  }
}

class TaskAdapter() {
  val logger = Logger("webApp")
  val userDao = new UserDAO()
  def toTaskAdapter(tasks: Future[Seq[EventTask]]): Future[Seq[EventTaskAdapter]] = {
    val tasksFuture = tasks.flatMap(seq => {
      val usersFuture = userDao.getUsersByIds(seq.map(_.userId.getOrElse(0)).distinct)
      usersFuture.flatMap(users => {
        Future {
          seq.map(task => {
            val user = users.find(_.id == task.userId)
            EventTaskAdapter(task.message, task.eventId, user.map(u => u.copy(clientId = None, id = None)),task.id)
          })
        }
      })
    })
    tasksFuture.recoverWith {
      case e: Throwable =>
        logger.debug("exception task adapter", e)
        Future.successful(Seq.empty[EventTaskAdapter])
    }
  }
}

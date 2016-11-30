package dao

import entities.db.{Tables, EventTask}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ivan on 20.11.16.
  */
class TaskDao extends DatabaseDAO[EventTask,Int] {
  private val table = Tables.tasks

  override def create(r: EventTask): Future[EventTask]= {
    val insert = (table returning table.map(_.id)).into( (item,id) => item.copy(id = Some(id)))
    execute(insert += r)
  }
  def createTasks(tasks: Seq[EventTask]): Future[Seq[EventTask]] = {
    execute((table returning table.map(_.id)).into((item, id) => item.copy(id = Some(id))) ++= tasks)
  }
  def getEventTasks(eId: Int) = {
    execute(table.filter(_.eventId === eId).result)
  }
  override def update(r: EventTask): Future[Int]  = {
    println("GOTTA UPDATE " + r)
    val query = for { task <- table if task.id === r.id } yield (task.userId, task.message)
    val action = query.update((r.userId, r.message))
    execute(action)
  }


  override def get(id: Int): Future[EventTask] = execute(table.filter(_.id === id).result.head)

  override def delete(r: EventTask): Future[Int] = execute(table.filter(_.id === r.id).delete)
}

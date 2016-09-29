package dao
import entities.db.{DatabaseExecutor, Tables, User}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ivan on 16.09.16.
  */
class UserDAO extends DatabaseDAO[User,Int]{
  private val table = Tables.users

  def count = execute(table.length.result)
  override def create(r: User): Future[User] = {
    val insert = (table returning table.map(_.id)).into( (item,id) => item.copy(id = Some(id)))
    execute(insert += r)
  }

  override def update(r: User): Future[Int] = {
    val query = table.filter(_.id === r.id)
    val action = query.update(r)
    execute(action)
  }

  //TODO \find for unique fields
  override def get(id: Int): Future[User] = execute(table.filter(_.id === id).result.head)
  def getByClientId(clientId: String): Future[User] = execute(table.filter(_.clientId === clientId).result.head)

  override def delete(r: User): Future[Int] = execute(table.filter(_.clientId === r.clientId).delete)
}

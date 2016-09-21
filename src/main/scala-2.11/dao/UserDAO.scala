package dao
import entities.{DatabaseHelper, Tables, User}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by ivan on 16.09.16.
  */
class UserDAO extends DatabaseDAO[User,String]{
  private val table = Tables.users

  override def create(r: User): Future[User] = {
    val insert = (table returning table.map(_.token)).into((item,token)=>item.copy(token=token))
    execute(insert += r)
  }

  override def update(r: User): Future[Int] = {
    val query = table.filter(_.token === r.token)
    val action = query.update(r)
    execute(action)
  }
  override def get(token: String): Future[User] = execute(table.filter(_.token === token).result.head)

  override def delete(r: User): Future[Int] = execute(table.filter(_.token === r.token).delete)
}

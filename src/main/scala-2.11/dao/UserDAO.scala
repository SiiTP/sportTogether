package dao
import entities.{DatabaseHelper, Tables, User}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 16.09.16.
  */
class UserDAO {
  def create(token:String,role:String): Unit ={
    val db = DatabaseHelper.getInstance
    val u = User(token,role)
    val result = db.run(DBIO.seq(Tables.users += u))
    result.onSuccess{
      case _ => println("success insertion " + token + ' ' + role)
    }
  }

}

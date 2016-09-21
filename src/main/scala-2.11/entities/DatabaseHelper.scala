package entities
import java.io.FileReader
import java.net.URL
import java.util.Properties

import entities.Tables._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by ivan on 15.09.16.
  */
object DatabaseHelper {
  private val db = Database.forConfig("mysqlDB")
  def getInstance = db
  def getTestInstance(url: URL) = Database.forURL(url.getPath)
  def init(configPath: String) = {
    val properties = new Properties()
    properties.load(new FileReader(configPath))
    val property: String = properties.getProperty("mysqlDB")
    println(property)
    properties.getProperty("db") match {
      case "create" => recreate()
      case _ => None
    }

  }
  def create(): Unit ={

    if(!isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).create
      )),Duration.Inf)
      println("DATABASE CREATED")
    }
  }
  def isCreated = Await.result(db.run(MTable.getTables), Duration.Inf).nonEmpty
  def recreate(): Unit ={
//    val p = new Properties()
//    p.load(new FileReader("application.conf"))
    drop()
    create()
  }
  def drop(): Unit ={
    if(isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).drop
      )),Duration.Inf)
      println("DATABASE DROPPED")
    }
  }
}

package entities
import java.io.FileReader
import java.util.Properties
import entities.Tables._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by ivan on 15.09.16.
  */
class DatabaseHelper(val config:String) {
  private val db = Database.forConfig(config)
  def getInstance = db
  def init(workingDirPath: String) = {
    println(workingDirPath)
    val properties = new Properties()
    properties.load(new FileReader(workingDirPath +'/' + "application.conf"))
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

package entities.db

import java.io.{InputStream, File, FileReader}
import java.util.Properties

import com.typesafe.config.ConfigFactory
import entities.db.Tables._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by ivan on 15.09.16.
  */
class DatabaseHelper private(){
  lazy private val db = DatabaseExecutor.getInstance
  def init(stream: InputStream) = {
//    println(configPath)
    val properties = new Properties()
//    properties.load(new FileReader(configPath))
    properties.load(stream)
    properties.getProperty("db") match {
      case "create" => recreate()
      case _ => None
    }

  }
  def create(): Unit ={
    this.synchronized {
      if(!isCreated){
        Await.result(db.run(DBIO.seq(
          (users.schema ++ events.schema ++ categories.schema ++ userReports.schema ++ eventUsers.schema).create
        )), Duration.Inf)
        println("DATABASE CREATED")
      }
    }
  }
  private def isCreated = Await.result(db.run(MTable.getTables), Duration.Inf).nonEmpty
  def recreate(): Unit ={
    this.synchronized {
      drop()
      create()
    }
  }
  def clearTables = Await.result(db.run(
    DBIO.seq(
      users      . delete,
      events     . delete,
      categories . delete,
      userReports. delete,
      eventUsers . delete
    )), Duration.Inf)

  def drop(): Unit ={
    this.synchronized{
      if(isCreated){
  //      Await.result(db.run(sqlu"""SET foreign_key_checks = 0 """),Duration.Inf)
        Await.result(db.run(DBIO.seq(
          (users.schema ++ events.schema ++ categories.schema ++ userReports.schema).drop
        )), Duration.Inf)
  //      Await.result(db.run(sqlu"""SET foreign_key_checks = 1 """),Duration.Inf)
        println("DATABASE DROPPED")
      }
    }
  }
}
object DatabaseHelper {
  private lazy val dbHelper = new DatabaseHelper()
  def getInstance = dbHelper
}
object DatabaseExecutor {
  lazy val db : Database = Database.forConfig(configPath, config)
  private var configPath: String = "mysqlDB"
  private var config = ConfigFactory.load()
  def config(configName: String, configFile : File = null): Unit = {
    configPath = configName
    if(configFile != null)
      config = ConfigFactory.parseFile(configFile)
  }
  def getInstance = db
}
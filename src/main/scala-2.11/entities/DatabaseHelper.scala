package entities
import java.io.{File, FileReader}
import java.net.URL
import java.util.Properties

import com.typesafe.config.ConfigFactory
import entities.Tables._
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by ivan on 15.09.16.
  */
class DatabaseHelper{
  lazy private val db = DatabaseHelper.getInstance
  def init(configPath: String) = {
    println(configPath)
    val properties = new Properties()
    properties.load(new FileReader(configPath))
    properties.getProperty("db") match {
      case "create" => recreate()
      case _ => None
    }

  }
  def create(): Unit ={

    if(!isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).create
      )), Duration.Inf)
      println("DATABASE CREATED")
    }
  }
  def isCreated = Await.result(db.run(MTable.getTables), Duration.Inf).nonEmpty
  def recreate(): Unit ={
    drop()
    create()
  }
  def clearTables = Await.result(db.run(
    DBIO.seq(
      users     . delete,
      events    . delete,
      categories. delete
    )), Duration.Inf)

  def drop(): Unit ={
    if(isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).drop
      )), Duration.Inf)
      println("DATABASE DROPPED")
    }
  }
}

object DatabaseHelper{
  lazy val db : Database = Database.forConfig(configPath, config)
  private var configPath: String = "mysqlDB"
  private var config = ConfigFactory.load()
  def config(configName: String, configFile : File = null): Unit = {
    configPath = configName
    if(configFile != null)
      config = ConfigFactory.parseFile(configFile)
  }
  def getInstance = {
    db
  }
}
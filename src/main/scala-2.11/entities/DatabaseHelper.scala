package entities

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
  def create(): Unit ={

    if(!isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).create
      )),Duration.Inf)
      println("DATABASE CREATED")
    }
  }
  def isCreated = Await.result(db.run(MTable.getTables), Duration.Inf).nonEmpty

  def drop(): Unit ={
    if(isCreated){
      Await.result(db.run(DBIO.seq(
        (users.schema ++ events.schema ++ categories.schema).drop
      )),Duration.Inf)
      println("DATABASE DROPPED")
    }
  }
}

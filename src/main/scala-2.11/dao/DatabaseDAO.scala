package dao

import entities.db.DatabaseHelper
import slick.dbio.{DBIOAction, NoStream}

/**
  * Created by ivan on 19.09.16.
  */
abstract class DatabaseDAO[R,F] extends CRUDActions[R,F]{
  private val db = DatabaseHelper.getInstance
  def execute[T](a: DBIOAction[T, NoStream, Nothing]) = db.run(a)
}

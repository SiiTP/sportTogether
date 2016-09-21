package dao

import scala.concurrent.Future

/**
  * Created by ivan on 19.09.16.
  */
trait CRUDActions[R,F] {
  def create(r:R) :Future[R]
  def delete(r:R) :Future[Int]
  def update(r:R) :Future[Int]
  def get(r:F) :Future[R]
}

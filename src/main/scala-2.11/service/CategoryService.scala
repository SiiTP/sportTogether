package service

import akka.actor.Actor
import dao.CategoryDAO
import entities.db.MapCategory
import response.CategoryResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
/**
  * Created by ivan on 27.09.16.
  */
class CategoryService {
  val categoryDAO = new CategoryDAO()
  def createCategory(name: String) = categoryDAO.create(MapCategory(name))
  def getAllCategories = categoryDAO.getCategories
  def getCategoryById(id: Int) = categoryDAO.get(id)
}

object CategoryService{
  case class GetCategories()
  case class GetCategory(id: Int)
  case class CreateCategory(name:String)
}
class CategoryServiceActor(categoryService: CategoryService) extends Actor{
  import CategoryService._
  override def receive: Receive = {
    case CreateCategory(name) =>
      val sended = sender()
      categoryService.createCategory(name).onComplete {
        case Success(category) => sended ! CategoryResponse.responseSuccess(Some(category))
        case Failure(t) => sended ! CategoryResponse.alreadyExistError
      }
    case GetCategories() =>
      val sended = sender()
      categoryService.getAllCategories.onComplete {
        case Success(cateogries) => sended ! cateogries
      }
    case GetCategory(id) =>
      val sended = sender()
      categoryService.getCategoryById(id).onComplete {
        case Success(category) => sended ! CategoryResponse.responseSuccess(Some(category))
        case Failure(t) => sended ! CategoryResponse.notFoundError
      }
  }
}

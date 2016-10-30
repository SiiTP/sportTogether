package service

import akka.actor.Actor
import dao.CategoryDAO
import entities.db.MapCategory
import response.CategoryResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import response.MyResponse._
import entities.db.EntitiesJsonProtocol._
/**
  * Created by ivan on 27.09.16.
  */
class CategoryService {

  val categoryDAO = new CategoryDAO()
  def getCategoriesByPartOfName(name: String) = categoryDAO.getCategoriesByPartOfName(name)

  def getEventsByCategoryName(name: String) = categoryDAO.eventsByCategoryName(name)
  def createCategory(name: String) = categoryDAO.create(MapCategory(name))
  def getAllCategories = categoryDAO.getCategories
  def getCategoryById(id: Int) = categoryDAO.get(id)
}

object CategoryService{
  case class GetCategories()
  case class GetCategoriesByPartOfName(name: String)
  case class GetCategory(id: Int)
  case class CreateCategory(name: String)

  case class GetEventsByCategoryName(name: String)

}
class CategoryServiceActor(categoryService: CategoryService) extends Actor{
  import CategoryService._
  override def receive: Receive = {
    case CreateCategory(name) =>
      val sended = sender()
      categoryService.createCategory(name).onComplete {
        case Success(category) => sended ! CategoryResponse.responseSuccess(Some(category)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.alreadyExistError.toJson.prettyPrint
      }

    case GetCategories() =>
      val sended = sender()
      categoryService.getAllCategories.onComplete {
        case Success(cateogries) => sended ! CategoryResponse.responseSuccess(Some(cateogries)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.unexpectedError.toJson.prettyPrint
      }

    case GetCategoriesByPartOfName(name) =>
      val sended = sender()
      categoryService.getCategoriesByPartOfName(name).onComplete {
        case Success(cateogries) => sended ! CategoryResponse.responseSuccess(Some(cateogries)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.unexpectedError.toJson.prettyPrint
      }

    case GetCategory(id) =>
      val sended = sender()
      categoryService.getCategoryById(id).onComplete {
        case Success(category) => sended ! CategoryResponse.responseSuccess(Some(category)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.notFoundError.toJson.prettyPrint
      }

    case GetEventsByCategoryName(name) =>
      val sended = sender()
      categoryService.getEventsByCategoryName(name).onComplete {
        case Success(result) =>
          sended ! CategoryResponse.responseSuccess(Some(result)).toJson.prettyPrint
        case Failure(t) => sended ! CategoryResponse.notFoundError.toJson.prettyPrint
      }
  }
}

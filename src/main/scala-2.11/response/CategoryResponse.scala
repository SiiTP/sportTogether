package response

/**
  * Created by ivan on 27.09.16.
  */
object CategoryResponse extends MyResponse{
  def alreadyExistError = ResponseError(1, "Категория уже существует")
  def notFoundError = ResponseError(2, "Категория не найдена")
}

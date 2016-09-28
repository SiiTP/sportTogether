package response

/**
  * Created by ivan on 27.09.16.
  */
object CategoryResponse extends MyResponse{
  def alreadyExistError = ResponseError(1, "category already exist")
  def notFoundError = ResponseError(2, "category not found")
}

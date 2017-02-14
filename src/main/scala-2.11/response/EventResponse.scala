package response

/**
  * Created by ivan on 29.09.16.
  */
object EventResponse extends MyResponse{
  def notFoundError = ResponseError(1, "Событие не найдено")
  def alreadyReport = ResponseError(2, "Вы уже пожаловались на событие")
  def noSomeParameters = ResponseError(3, "no some required parameters in request")
  def alreadyPostedResult = ResponseError(4, "Результат уже подведен!")
  def incorectDate = ResponseError(5, "Неверная дата!")
  def notYourEvent = ResponseError(6, "Вы не являетесь создателем события")
  def alreadyDeleted = ResponseError(7, "Событие уже удалено")
}

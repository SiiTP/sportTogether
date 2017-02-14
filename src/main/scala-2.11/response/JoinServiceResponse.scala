package response

/**
  * errror codes 600-700
  */
object JoinServiceResponse extends MyResponse{
  def userAlreadyJoined = ResponseError(601,"Вы уже присоединены")
  def eventIsFull = ResponseError(602,"Событие заполнено")
  def userNotFoundInEvent = ResponseError(603,"Такого пользователя нет в событии")
}

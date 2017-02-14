package response

/**
  * Created by ivan on 20.11.16.
  */
object TaskResponse extends MyResponse{
  def somebodyAccepted = ResponseError(801, "Кто-то уже подтвердил это задание")
  def mustBeJoinedToEvent = ResponseError(802, "Вы должны быть присоединены к событию")

}

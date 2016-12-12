package response

/**
  * Created by ivan on 20.11.16.
  */
object TaskResponse extends MyResponse{
  def somebodyAccepted = ResponseError(801, "somebody accepted task")
  def mustBeJoinedToEvent = ResponseError(802, "you must be joined to event")

}

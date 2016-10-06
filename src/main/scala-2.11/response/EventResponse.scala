package response

/**
  * Created by ivan on 29.09.16.
  */
object EventResponse extends MyResponse{
  def notFoundError = ResponseError(1, "event not found")
  def alreadyReport = ResponseError(2, "event already reported")
}

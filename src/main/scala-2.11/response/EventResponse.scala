package response

/**
  * Created by ivan on 29.09.16.
  */
object EventResponse extends MyResponse{
  def notFoundError = ResponseError(1, "event not found")
  def alreadyReport = ResponseError(2, "event already reported")
  def noSomeParameters = ResponseError(3, "no some required parameters in request")
  def alreadyPostedResult = ResponseError(4, "result already posted!")
  def incorectDate = ResponseError(5, "incorrect date!")
}

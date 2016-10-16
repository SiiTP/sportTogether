package response

/**
  * errror codes 600-700
  */
object JoinServiceResponse extends MyResponse{
  def userAlreadyJoined = ResponseError(601,"User already joined this event")
  def eventIsFull = ResponseError(602,"Event is full")
}

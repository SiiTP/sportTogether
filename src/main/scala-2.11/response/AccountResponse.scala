package response



/**
  * Created by root on 24.09.16.
  */
object AccountResponse extends MyResponse {
  val CODE_NOT_AUTHORIZED = 10
  val CODE_AUTH_ALREADY = 11

  def responseNotAuthorized = ResponseError(AccountResponse.CODE_NOT_AUTHORIZED, "you are not authorized")
  def responseAlreadyAuthorized = ResponseError(AccountResponse.CODE_AUTH_ALREADY, "you are already authorized")
}

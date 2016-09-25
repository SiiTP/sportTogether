package response

/**
  * Created by root on 24.09.16.
  */
object AccountResponse {
  val CODE_NOT_AUTHORIZED = 10
  //  val CODE_NOT_AUTHORIZED_TIMEOUT = 2
  //
  val CODE_AUTH_ALREADY = 11
  //  val CODE_AUTH_SUCCESSFUL = 11
  //  val CODE_AUTH_UNSUCCESSFUL = 12
  //
  //  val CODE_REG_SUCCESSFUL = 20
  //  val CODE_REG_ACC_EXIST = 21

}

trait AccountResponse extends MyResponse {
  def responseNotAuthorized = ResponseError(AccountResponse.CODE_NOT_AUTHORIZED, "you are not authorized")
  def responseAlreadyAuthorized = ResponseError(AccountResponse.CODE_AUTH_ALREADY, "you are already authorized")
}

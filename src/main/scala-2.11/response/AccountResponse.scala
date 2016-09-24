package response

/**
  * Created by root on 24.09.16.
  */
trait AccountResponse extends MyResponse {
  val CODE_NOT_AUTHORIZED = 1
  //  val CODE_NOT_AUTHORIZED_TIMEOUT = 2
  //
  val CODE_AUTH_ALREADY = 10
  //  val CODE_AUTH_SUCCESSFUL = 11
  //  val CODE_AUTH_UNSUCCESSFUL = 12
  //
  //  val CODE_REG_SUCCESSFUL = 20
  //  val CODE_REG_ACC_EXIST = 21
  case class ResponseNotAuthorized() extends ResponseError(CODE_NOT_AUTHORIZED, "you are not authorized")
  case class ResponseAlreadyAuthorized() extends ResponseError(CODE_AUTH_ALREADY, "you are already authorized")
}

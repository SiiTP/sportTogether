package response



/**
  * Created by root on 24.09.16.
  */
object AccountResponse extends MyResponse {
  val CODE_NOT_AUTHORIZED = 10
  val CODE_AUTH_ALREADY = 11
  val CODE_USER_NOT_FOUND = 12

  def responseNotAuthorized = ResponseError(AccountResponse.CODE_NOT_AUTHORIZED, "Вы не авторизованы")
  def responseAlreadyAuthorized = ResponseError(AccountResponse.CODE_AUTH_ALREADY, "Вы авторизованы")
  def responseUpdateFailed = ResponseError(AccountResponse.CODE_USER_NOT_FOUND, "Пользователь не найден")
}

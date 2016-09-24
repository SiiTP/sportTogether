package response

/**
  * Created by root on 24.09.16.
  */
trait MyResponse {
  val CODE_SUCCESS = 0

  case class ResponseSuccess[T](code: Int = CODE_SUCCESS, message: String = "success", data: T)
  case class ResponseError(code: Int, message: String)

}

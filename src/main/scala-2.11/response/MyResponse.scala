package response

import spray.json.DefaultJsonProtocol

/**
  * Created by root on 24.09.16.
  */
object MyResponse {
  val CODE_SUCCESS = 0
  val CODE_NOT_SUCCESS = 1
}

trait MyResponse {
  case class ResponseSuccess[T](code: Int = MyResponse.CODE_SUCCESS, message: String, data: T = None)
  case class ResponseError(code: Int, message: String)

  def responseSuccess[T](data: T) = ResponseSuccess[T](MyResponse.CODE_SUCCESS, "Success!", data)
  def responseError(code: Int, message: String) = ResponseError(code, message)
}

object ResponseJsonProtocol extends DefaultJsonProtocol with MyResponse {
  implicit val errorFormat = jsonFormat2(ResponseError)
  //    implicit val successFormat = jsonFormat4(ResponseSuccess[User])
}



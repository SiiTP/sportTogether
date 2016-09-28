package response

import entities.db.User
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, JsonFormat}

//import spray.json._

/**
  * Created by root on 24.09.16.
  */
object MyResponse {
  val CODE_SUCCESS = 0
  val CODE_NOT_SUCCESS = 1
}

trait MyResponse extends DefaultJsonProtocol {
  implicit val errorFormat = jsonFormat2(ResponseError)
  implicit def successFormat[T: JsonFormat] = jsonFormat3(ResponseSuccess.apply[T])

  case class ResponseSuccess[T](code: Int = MyResponse.CODE_SUCCESS, message: String, data: Option[T] = None)
  case class ResponseError(code: Int, message: String)

  def responseSuccess[T](data: Option[T]) = ResponseSuccess[T](MyResponse.CODE_SUCCESS, "Success!", data)
  def responseError(code: Int, message: String) = ResponseError(code, message)
  def responseNotSuccess() = responseError(MyResponse.CODE_NOT_SUCCESS, "Not success")
}

//object ResponseJsonProtocol extends DefaultJsonProtocol with MyResponse {
//  implicit val errorFormat = jsonFormat2(ResponseError)
//  implicit def successFormat[T: JsonFormat] = jsonFormat3(ResponseSuccess.apply[T])
//}



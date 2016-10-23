package service

import response.CategoryResponse

import scala.collection.mutable
import scala.util.{Failure, Success}

import scala.concurrent.Future

import akka.actor.{ActorContext, Actor}
import akka.actor.Actor.Receive
import com.typesafe.scalalogging.Logger
import service.FcmService.SendMessage
import spray.client.pipelining._
import spray.http.{MediaTypes, HttpEntity, HttpHeader, HttpResponse}
import spray.json._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by ivan on 08.10.16.
  */
class FcmService {
//  val apiKey = "AIzaSyChprfOIcFm1yn0R-h8IrcQ3WBRh20A3rg"//test key
  val apiKey = "AIzaSyAiLt5o7FNAZva-fkITjzyG7mejQopwYss"//technopark api key
  val logger = Logger("webApp")

  /**
    *
    * @param tokens токены юзеров, которым отправляем сообщение
    * @param message json сообщение { *** }
    * @param context необходим для sendReceive
    * @return массив будущих ответов
    */
  def sendMessage(tokens: Seq[String], message: JsObject, context: ActorContext): Future[mutable.ArraySeq[HttpResponse]] = {
    implicit val messageFormat = jsonFormat2(MessageTo)
    implicit val actorSystem = context.system
    val pipeline = sendReceive
    var responses: mutable.ArraySeq[Future[HttpResponse]] = new mutable.ArraySeq[Future[HttpResponse]](0)
    tokens.foreach(item => {
      val request = Post("https://fcm.googleapis.com/fcm/send",HttpEntity(MediaTypes.`application/json`,MessageTo(item, message).toJson.compactPrint)) ~> addHeader("Authorization",s"key=$apiKey")
      //~> addHeader("Content-Type","application/json")
      logger.debug("sending fcm headers " + request.headers + "\nWith data: " + request.entity.data.asString)
      val response = pipeline {
        request
      }
      responses = responses :+ response
    })
    logger.debug(s"Sended $responses.length \n$responses")
    Future.sequence(responses)
  }
}

case class MessageTo(to: String, data: JsObject)
object FcmService {
  case class SendMessage(deviceToken: Seq[String], data: JsObject)
}
class FcmServiceActor extends Actor {
  val logger = Logger("webApp")
  val service = new FcmService()
  override def receive: Receive = {
    case msg: SendMessage =>
      val sended = sender()
      service.sendMessage(msg.deviceToken,msg.data, context).onComplete {
        case Success(result) =>
          result.foreach(item => logger.debug("Got response FCM: \nCode: "+ item.status + "\nHeader: "+ item.headers + " \nWith data: " + item.entity.data.asString))
          sended ! CategoryResponse.responseSuccess(Some(s"sended ${result.length} messages")).toJson.prettyPrint
        case Failure(t) =>
          logger.debug("error" , t)
          sended ! CategoryResponse.unexpectedError.toJson.prettyPrint
      }
  }
}

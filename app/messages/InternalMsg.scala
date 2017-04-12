package messages

import java.util.UUID

import play.api.libs.json._

/**
  * Internal messages are used inside the server to publish data
  * (carrying a json messages) to connected clients.
  *
  * @param message the json message
  * @param accId   the user account id
  * @param topic   the topic the msg belongs to
  * @param created the timestamp at creation
  */
case class InternalMsg(message: JsValue, accId: UUID, topic: String, created: String)

object InternalMsg {
  implicit val fmt = Json.format[InternalMsg]
}


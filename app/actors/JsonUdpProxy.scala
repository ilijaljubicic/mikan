package actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Udp}
import akka.util.ByteString
import db.DataBaseAccess
import models.Account
import play.api.libs.json.JsValue
import play.libs.Json

import scala.collection.mutable

/**
  * receive UDP ByteString, translate them to json and pass them onto a ClientSocket actor,
  * and
  * send the ClientSocket responses as UPD ByteString to the client
  */
class JsonUdpProxy(val account: Account, val clientList: mutable.Map[String, ActorRef],
                   val out: ActorRef, val mediator: ActorRef, val dbService: ActorRef,
                   val dbAccess: DataBaseAccess) extends Actor {

  import context.system

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.JsonUdpProxy")

  // the actor that will do all json processing
  val clientSocket = system.actorOf(ClientSocket.props(account, clientList)(self, mediator, dbService, dbAccess))

  var theAddress: InetSocketAddress = _

  def receive: Receive = {

    // receive a ByteString from the UDP connection to send to the clientSocket as json
    case Udp.Received(data, remote) =>
      theAddress = remote
      clientSocket !  Json.parse(data.toArray)

    // receive json msg from the clientSocket to send to the client as ByteString
    case js: JsValue => out ! Udp.Send(ByteString(js.toString), theAddress)

  }
}

object JsonUdpProxy {

  def props(acc: Account, clientList: mutable.Map[String, ActorRef])(out: ActorRef, mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) =
   Props(new JsonUdpProxy(acc, clientList, out, mediator, dbService, dbAccess))

}

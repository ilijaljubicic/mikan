package actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import db.DataBaseAccess
import messages.ServerMsg
import models.Account

import scala.collection.mutable
import scala.language.implicitConversions


// todo
/**
  * Manage the server activities from a websocket connection.
  *
  */
class ServerManager(val account: Account, val clientList: mutable.Map[UUID, ActorRef],
                    val out: ActorRef, val mediator: ActorRef, val dbService: ActorRef,
                    val dbAccess: DataBaseAccess) extends Actor with ActorLogging {

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.ServerManager")

  // process the server messages
  def receive: Receive = LoggingReceive {

    // a ServerMsg
    case serverMsg: ServerMsg => logger.info(s"received serverMsg: $serverMsg")

    // some unknown message
    case x => logger.info(s"received a non ServerMsg: $x")
  }

}

object ServerManager {

  def props(acc: Account, clientList: mutable.Map[UUID, ActorRef])(out: ActorRef, mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) =
    Props(new ServerManager(acc, clientList, out, mediator, dbService, dbAccess))

}
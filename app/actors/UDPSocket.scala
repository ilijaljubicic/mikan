package actors

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.io.{IO, Udp}
import db.DataBaseAccess
import models.Account

import scala.collection.mutable
import scala.util.Random

/**
  * manage UDP socket connections for clients exchanging messages.
  *
  */
class UDPSocket(val clientList: mutable.Map[UUID, ActorRef],
                val mediator: ActorRef, val dbService: ActorRef,
                val dbAccess: DataBaseAccess) extends Actor {

  import context.system

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.UDPSocket")

  // this actor will receive all Bound messages
  // self here could be another actor that will receive all datagrams (not the Bound messages)
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 0))

  def receive: Receive = {
    // have a connection
    case Udp.Bound(local) =>
      // todo --> for testing create a random User account
      val userAccount = new Account(UUID.randomUUID(), Account.User, "testname")
      val userSocketProxy = system.actorOf(
        JsonUdpProxy.props(userAccount, clientList)(sender(), mediator, dbService, dbAccess)
      )
      context become ready(sender(), userSocketProxy)
  }

  def ready(out: ActorRef, proxy: ActorRef): Receive = {

    // forward all data to the proxy
    case Udp.Received(data, remote) => proxy forward Udp.Received(data, remote)

    case Udp.Unbind =>
      out ! Udp.Unbind
      proxy ! PoisonPill

    case Udp.Unbound => context.stop(self)
  }
}

object UDPSocket {

  def props(clientList: mutable.Map[UUID, ActorRef])(mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) =
    Props(new UDPSocket(clientList, mediator, dbService, dbAccess))

}


package actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Udp}


// todo
/**
  * manage a socket to DIS simulations and send the data for processing.
  *
  *
  * see also: https://github.com/typesafehub/activator-akka-stream-java8/blob/master/src/main/java/sample/stream/TcpTLSEcho.java
  *
  */
class DisSocket extends Actor {

  import context.system

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.DisSocket")

  // this actor will receive all Bound messages
  // self here could be another actor that will receive all datagrams (not the Bound messages)
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("localhost", 0))

  def receive: Receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {

    case Udp.Received(data, remote) =>
      println("-----------> in DisSocket received data: " + data.toString())

    case Udp.Unbind => socket ! Udp.Unbind

    case Udp.Unbound => context.stop(self)
  }
}

object DisSocket {

  def props() = Props(new DisSocket())

}
package actors

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSubMediator._
import akka.event.LoggingReceive
import db.DataBaseAccess
import messages.InternalMsg
import messages.Mikan._
import models.{Account, FilterJsonMsg}
import play.api.libs.json.{JsValue, _}

import scala.collection.mutable
import scala.language.implicitConversions


/**
  * A json message publish-subscribe actor.
  *
  * This actor will publish all json messages it receives (except the commands MikanMsg types),
  * and
  * send to its client all json messages it is subscribed to
  */
class ClientSocket(val account: Account, val clientList: mutable.Map[String, ActorRef],
                   val out: ActorRef, val mediator: ActorRef, val dbService: ActorRef,
                   val dbAccess: DataBaseAccess) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    clientList += account.name -> self
    super.preStart()
  }

  override def postStop(): Unit = {
    clientList -= account.name
    super.postStop()
  }

  val logger = org.slf4j.LoggerFactory.getLogger("actors.ClientSocket")

  // the filtering engine
  var filterEngine: Option[FilterJsonMsg] = None

  // the name of the default topic to subscribe to
  val defaultTopic = "json"

  // the current topic to subscribed to
  var subsTopic = defaultTopic

  // the current topic to publish to
  var pubTopic = defaultTopic

  // todo must supervise the child actors

  // subscribe to the defaultTopic to start with
  mediator ! Subscribe(defaultTopic, self)

  // receive the client json messages
  def receive: Receive = LoggingReceive {

    // acknowledge subscription to a topic
    case SubscribeAck(Subscribe(topic, None, `self`)) => logger.info(s"subscribed to: $topic")

    // acknowledge unsubscribe from a topic
    case UnsubscribeAck(Unsubscribe(topic, None, `self`)) => logger.info(s"unsubscribe from: $topic")

    // process the json msg received from this client connection
    case msg: JsValue =>
      Json.fromJson[MikanMsg](msg).asOpt match {

        case Some(mikan: MikanSubscribe) =>
          // unsubscribe from the last topic
          mediator ! Unsubscribe(subsTopic, self)
          // must not have an empty topic
          subsTopic = if (mikan.topic.isEmpty) defaultTopic else mikan.topic
          mediator ! Subscribe(subsTopic, self)

        case Some(mikan: MikanPublish) =>
          pubTopic = if (mikan.topic.isEmpty) defaultTopic else mikan.topic

        case Some(mikan: MikanFilter) =>
          // if the msg contains a filter, create a filter engine with the given script
          if (mikan.script.isEmpty) filterEngine = None // this turns the filter off
          else filterEngine = Option(new FilterJsonMsg(mikan.script))

        // not a command msg
        case None =>
          // create an InternalMsg to carry the msg
          val internalMsg = InternalMsg(msg, account.accId, pubTopic, LocalDateTime.now().toString)
          // store the message
          if (dbAccess.withDatabase) dbService ! internalMsg
          // publish the message
          mediator ! Publish(pubTopic, internalMsg)
      }

    // send to this client the json msg from all other publisher clients (subject to filtering)
    // note: only InternalMsg with the topic this client is subscribed to arrive here
    case msg: InternalMsg =>
      // do not send me back the msg I published
      if (msg.accId != account.accId) {
        filterEngine match {
          // no filter
          case None => out ! Json.toJson(msg.message)
          // apply the filter to the message
          case Some(filter) => if (filter.accept(msg.message)) out ! Json.toJson(msg.message)
        }
      }

    case x => logger.info(s"received an unknown message type: $x")
  }

}

object ClientSocket {

  def props(acc: Account, clientList: mutable.Map[String, ActorRef])(out: ActorRef, mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) =
    Props(new ClientSocket(acc, clientList, out, mediator, dbService, dbAccess))

}



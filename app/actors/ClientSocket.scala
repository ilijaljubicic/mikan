package actors

import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSubMediator._
import akka.event.LoggingReceive
import db.DataBaseAccess
import messages.InternalMsg
import messages.Mikan._
import models.{Account, FilterBuilder, FilterJsonMsg}
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

  // the current topic list subscribed to, should always include the default topic
  val subsTopicList = mutable.ListBuffer[String](defaultTopic)

  // the current topic to publish to, either the default or a named topic, never empty
  var pubTopic = defaultTopic

  // todo must supervise the child actors

  // subscribe to the defaultTopic to start with
  mediator ! Subscribe(defaultTopic, self)

  // receive the client json messages
  def receive: Receive = LoggingReceive {

    // acknowledge subscription to a topic
    case SubscribeAck(Subscribe(topic, None, `self`)) => logger.info(s"SubscribeAck: $topic")

    // acknowledge unsubscribe from a topic
    case UnsubscribeAck(Unsubscribe(topic, None, `self`)) => logger.info(s"UnsubscribeAck: $topic")

    // process the json msg received from this client connection
    case msg: JsValue =>
      Json.fromJson[MikanMsg](msg).asOpt match {

        case Some(mikan: MikanSubscribe) =>
          if (mikan.topic.nonEmpty) {
            // unsubscribe to all topics
            subsTopicList.foreach(topic => mediator ! Unsubscribe(topic, self))
            subsTopicList.clear()
            // if all entries in the array of topics are empty, subscribe to the default topic
            if(mikan.topic.forall(_.isEmpty)) {
              subsTopicList += defaultTopic
              mediator ! Subscribe(defaultTopic, self)
            } else {
              // subscribe to the new list
              mikan.topic.foreach(topic => {
                if (topic.nonEmpty) {
                  subsTopicList += topic
                  mediator ! Subscribe(topic, self)
                }
              })
            }
          } else {
            // unsubscribe to all topics, but not the default topic
            subsTopicList.foreach(topic => if (topic != defaultTopic) mediator ! Unsubscribe(topic, self))
            subsTopicList.clear()
            // make sure the defaultTopic is in the list
            subsTopicList += defaultTopic
          }

        case Some(mikan: MikanPublish) =>
          pubTopic = if (mikan.topic.isEmpty) defaultTopic else mikan.topic

        case Some(mikan: MikanFilter) =>
          // if the msg contains a filter, create a filter engine with the given script
          if (mikan.script.isEmpty) filterEngine = None // this turns the filter off
          else filterEngine = FilterBuilder.createJsonFilter(mikan.script)

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
    // note: only InternalMsg with the topics this client is subscribed to arrive here
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

  private def unsubscribeToAll() = {
    subsTopicList.foreach(topic => mediator ! Unsubscribe(topic, self))
  }

}

object ClientSocket {

  def props(acc: Account, clientList: mutable.Map[String, ActorRef])(out: ActorRef, mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) =
    Props(new ClientSocket(acc, clientList, out, mediator, dbService, dbAccess))

}



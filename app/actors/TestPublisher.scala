package actors

import java.time.LocalDateTime
import java.util.UUID

import scala.util.Random
import scala.concurrent.duration._
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import db.DataBaseAccess
import messages.InternalMsg
import messages.SchedulerMsg._
import play.api.libs.json.Json


/**
  * A test publisher, publishing random InternalMsg.
  */
object TestPublisher {

  def props(mediator: ActorRef, dbService: ActorRef, dbAccess: DataBaseAccess) = Props(new TestPublisher(mediator, dbService, dbAccess))
}

class TestPublisher(val mediator: ActorRef, val dbService: ActorRef, val dbAccess: DataBaseAccess) extends Actor {

  import context.dispatcher

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.TestPublisher")

  def scheduler = context.system.scheduler

  // to be able to stop the scheduler
  var theScheduler: Cancellable = _

  override def preStart(): Unit = theScheduler = scheduler.scheduleOnce(5.seconds, self, Tick)

  override def postStop(): Unit = theScheduler.cancel()

  def receive = {

    case Stop => theScheduler.cancel()

    case Tick =>
      if (!theScheduler.isCancelled) {
        theScheduler = scheduler.scheduleOnce(5.seconds, self, Tick)

        // Sydney
        val newLat = -33.8688 + Random.nextDouble * 2.0
        val newLon = 151.2093 + Random.nextDouble * 2.0

        val msg = Json.obj("entity" -> "sim-aircraft", "milsymbol" -> "SUG-UCI----D", "lat" -> newLat, "lon" -> newLon)
        val internalMsg = InternalMsg(msg, UUID.randomUUID(), "json", LocalDateTime.now().toString)
        // store the message
        if (dbAccess.withDatabase) dbService ! internalMsg
        // publish the message
        mediator ! Publish("json", internalMsg)
      }
  }

}
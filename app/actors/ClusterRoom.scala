package actors

import akka.actor._
import akka.cluster.ClusterEvent.{InitialStateAsEvents, MemberEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.Cluster
import akka.event.LoggingReceive

/**
  * cluster event listener
  */
class ClusterRoom extends Actor with ActorLogging {

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.ClusterRoom")

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = LoggingReceive {

    case MemberUp(member) => logger.info(s"Member is Up: ${member.address} ")

    case UnreachableMember(member) => logger.info(s"Member detected as unreachable: $member ")

    case MemberRemoved(member, previousStatus) => logger.info(s"Member is Removed: ${member.address} after $previousStatus ")

    case _: MemberEvent => // ignore
  }
}
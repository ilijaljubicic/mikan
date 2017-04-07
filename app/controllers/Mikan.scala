package controllers

import java.util.UUID
import javax.inject._

import actors._
import akka.actor._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{RequestHeader, WebSocket}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.stream.Materializer
import db.{AccountDao, DataBaseAccess, MsgDao}
import models.Account
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}

import scala.util.Random
import scala.collection._


/**
  * Receives all connections requests.
  */
@Singleton
class Mikan @Inject()(val global: OnServerStart, val messagesApi: MessagesApi,
                      val reactiveMongoApi: ReactiveMongoApi,
                      val userDao: AccountDao, val msgDao: MsgDao)
                     (implicit val system: ActorSystem, val mat: Materializer, ec: ExecutionContext)
  extends MongoController with ReactiveMongoComponents with I18nSupport {

  private val logger = org.slf4j.LoggerFactory.getLogger("controllers.Mikan")

  // convenience access to the database access objects
  val dbAccess = DataBaseAccess(userDao, msgDao, global.withDatabase)

  // keep a list of currently connected clients
  val clientList = mutable.Map[String, ActorRef]()

  // todo must supervise the actors specially the websocket

  val clusterRoom: ActorRef = system.actorOf(Props[ClusterRoom], "cluster-room")

  // provide database services
  val dbService: ActorRef = system.actorOf(DBService.props(dbAccess))

  // the actor that manages a registry of actors and replicates
  // the entries to peer actors among all cluster nodes tagged with a specific role.
  val mediator: ActorRef = DistributedPubSub(system).mediator
  implicit val node = Cluster(system)

  // handle UDP connections
  val udpSocket: ActorRef = system.actorOf(UDPSocket.props(clientList)(mediator, dbService, dbAccess))

  // todo --> for testing ... a test publishing service
  //  val testPub: ActorRef = system.actorOf(TestPublisher.props(mediator, dbService, dbAccess))

  // todo --> for testing
  //  def index = Action.async { implicit request =>
  //   println("\n-------< request.host: "+request.host)
  //    Future(Ok(views.html.index()))
  //  }

  // todo --> create a random User account
  private def checkUser(request: RequestHeader): Option[Account] = {
    val userAccount = new Account("account_" + Random.nextInt(10000).toString, UUID.randomUUID().toString, Account.User)
    if (dbAccess.withDatabase) dbAccess.userDao.save(userAccount)
    Option(userAccount)
  }

  /**
    * A json message webSocket connection point,
    * will pass json messages to all "user" clients connected to this socket, subject to pub-sub topics,
    * also cater
    * for json server management messages from "admin" clients.
    */
  def mikanjson = WebSocket.acceptOrResult[JsValue, JsValue] {

    case request if sameOriginCheck(request) =>
      Future.successful(
        checkUser(request) match {
          case None => Left(Forbidden)
          case Some(account) =>
            logger.info(s"in mikanjson account: ${account.accId}")
            if (account.role == Account.Admin)
              Right(ActorFlow.actorRef(out => ServerManager.props(account, clientList)(out, mediator, dbService, dbAccess)))
            else
              Right(ActorFlow.actorRef(out => ClientSocket.props(account, clientList)(out, mediator, dbService, dbAccess)))
        })

    case rejected =>
      logger.error(s"Request $rejected failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  /**
    * checks that the WebSocket comes from the same origin.
    * To protect against Cross-Site WebSocket Hijacking as WebSocket
    * does not implement Same Origin Policy.
    */
  private def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value $badOrigin is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
    * returns true if the value of the Origin header contains an acceptable value.
    */
  private def originMatches(origin: String): Boolean = {
    val port = global.configuration.getInt("mikan.port", 9000)
    val host = global.configuration.getString("mikan.host", "0.0.0.0")
    logger.info("originMatches: host: $host port: $port")
    origin.contains(s"$host:$port") || origin.contains("file://") || origin.contains("http://localhost")
  }
}

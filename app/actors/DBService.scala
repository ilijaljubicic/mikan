package actors

import akka.actor.{Actor, ActorLogging, Props}
import db.DataBaseAccess
import messages.InternalMsg
import models.Account

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * provides database services for Accounts and InternalMsg
  *
  */
object DBService {

  def props(dbAccess: DataBaseAccess) = Props(new DBService(dbAccess))

  // message to update a user account
  case class UpdateAccount(account: Account)

}

class DBService(val dbAccess: DataBaseAccess) extends Actor with ActorLogging {

  import DBService._

  private val logger = org.slf4j.LoggerFactory.getLogger("actors.DBService")

  override def preStart(): Unit = ()

  override def postRestart(reason: Throwable): Unit = ()

  // receive commands for the database and collections
  def receive = {

    // save a user Account
    case user: Account =>
      logger.info("saving Account ")
      dbAccess.userDao.save(user) onComplete {
        case Success(result) => logger.info(s"save Account Success code: ${result.code} ")
        case Failure(t) => logger.info(s"save Account Failure ${t.getMessage} ")
      }

    // update a user Account
    case UpdateAccount(acc) =>
      logger.info("updating Account")
      dbAccess.userDao.update(acc) onComplete {
        case Success(result) => logger.info(s"update Account Success code: $result ")
        case Failure(t) => logger.info(s"update Account Failure ${t.getMessage} ")
      }

    // save a InternalMsg
    case msg: InternalMsg =>
      logger.info("saving InternalMsg ")
      dbAccess.msgDao.save(msg) onComplete {
        case Success(result) => logger.info(s"save InternalMsg Success code: ${result.code} ")
        case Failure(t) => logger.info(s"save InternalMsg Failure ${t.getMessage} ")
      }

  }

}

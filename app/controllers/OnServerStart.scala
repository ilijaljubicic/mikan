package controllers

import javax.inject._

import db.{AccountDao, MsgDao}
import play._
import scala.language.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class OnServerStart @Inject()(val configuration: Configuration, val userDao: AccountDao) {

  private val logger = org.slf4j.LoggerFactory.getLogger("controllers.OnServerStart")

  val withDatabase: Boolean = configuration.getBoolean("mikan.withdatabase", false)

  checkDbSetup()

  private def checkDbSetup() = {
    if (withDatabase) {
      // todo --> for testing, clear all Accounts when starting the server
      val clearAcc = userDao.clearAccounts().map(s => s)
      // wait here until done
      Await.ready(clearAcc, 30 seconds)
      val result = clearAcc onComplete {
        case Success(x) => logger.info("clearAccounts Success")
        case Failure(t) => logger.info(s"clearAccounts Failure ${t.getMessage} ")
      }
    }
  }

  // does not work never gets here
  def beforeStart(app: Application): Unit = {}
  // does not work never gets here
  def onStart(app: Application): Unit = {}
  // does not work never gets here
  def onStop(app: Application) {}
}
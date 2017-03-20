package db

import models.Account
import play.api.libs.json.{JsObject, Json}
import javax.inject.{Inject, Singleton}

import play.api.Configuration

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._

/**
  * implementation of the data access object for user Accounts
  */
@Singleton
class MongoAccountDao @Inject()(playConf: Configuration, val reactiveMongoApi: ReactiveMongoApi) extends AccountDao {

  private val logger = org.slf4j.LoggerFactory.getLogger("db.AccountDao")

  private val accountCol: String = playConf.getString("mongo.collection.accounts").getOrElse("accounts")

  private def accountsF: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection](accountCol))

  def find(accId: String): Future[Option[Account]] = for {
    accounts <- accountsF
    user <- accounts.find(Json.obj("accId" -> accId)).one[Account]
  } yield user

  def save(acc: Account): Future[WriteResult] = for {
      accounts <- accountsF
      lastError <- accounts.insert(acc)
    } yield lastError

  def update(acc: Account): Future[WriteResult] = {
    val doc = Json.toJson(acc).as[JsObject]
    accountsF.flatMap(_.update(Json.obj("accId" -> acc.accId), doc))
  }

  def clearAccounts(): Future[WriteResult] =  for {
      logs <- accountsF
      result <- logs.remove(Json.obj())
    } yield result

}



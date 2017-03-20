package db

import play.api.libs.json.{JsObject, Json}
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.play.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoApi
import messages.InternalMsg
import play.api.Configuration
import reactivemongo.api.{Cursor, ReadPreference}
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._


/**
  * DAO implementation for MsgDao
  *
  */
@Singleton
class MongoMsgDao @Inject()(playConf: Configuration, val reactiveMongoApi: ReactiveMongoApi) extends MsgDao {

  private val msgCol: String = playConf.getString("mongo.collection.clientmsg").getOrElse("clientmsg")

  private def msgF = reactiveMongoApi.database.map(_.collection[JSONCollection](msgCol))

  def find(accId: String): Future[Option[InternalMsg]] = {
    for {
      colection <- msgF
      msg <- colection.find(Json.obj("accId" -> accId)).one[InternalMsg]
    } yield msg
  }

  def save(msg: InternalMsg): Future[WriteResult] = {
    for {
      colection <- msgF
      lastError <- colection.insert(msg)
    } yield lastError
  }

  // get all current msg filtered by topicList and accId
  // note: if accId="" all objects will be retrieved, otherwise
  // all msg BUT NOT those with the given accId
  def findAll(topicList: List[String], accId: String): Future[List[InternalMsg]] = {
    val qry = Json.obj("accId" -> Json.obj("$ne" -> accId), "topic" -> Json.obj("$in" -> topicList))
    for {
      colection <- msgF
      theList <- colection.find(qry).
        cursor[InternalMsg](ReadPreference.nearest).
        collect[List](-1, Cursor.FailOnError[List[InternalMsg]]())
    } yield theList
  }

}

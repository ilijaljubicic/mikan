package db

import java.util.UUID

import com.google.inject.ImplementedBy
import messages.InternalMsg
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

@ImplementedBy(classOf[MongoMsgDao])
trait MsgDao {
  def save(msg: InternalMsg): Future[WriteResult]

  def find(msg: UUID): Future[Option[InternalMsg]]

  def findAll(objTypeList: List[String], accId: UUID): Future[List[InternalMsg]]
}

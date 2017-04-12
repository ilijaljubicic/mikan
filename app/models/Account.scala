package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * a client account
  *
  * @param accId a unique id for this account, used for in db
  * @param role either a user or admin account
  * @param name the name of the client
  * @param phone
  * @param latitude
  * @param longitude
  * @param email
  * @param description
  */
case class Account(accId: UUID, role: String, name: String, phone: Option[String] = None,
                   latitude: Option[Double] = None, longitude: Option[Double] = None,
                   email: Option[String] = None, description: Option[String] = None) {

  import Account._

  def isAdmin: Boolean = role == Admin

  def isUser: Boolean = role == User

}

object Account {
  val Admin = "admin"
  val User = "user"

  implicit val fmt = Json.format[Account]
}

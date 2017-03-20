package db

/**
  * for convenience keep the database access objects
  */
case class DataBaseAccess(userDao: AccountDao, msgDao: MsgDao, withDatabase: Boolean)

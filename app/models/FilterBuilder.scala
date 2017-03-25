package models


/**
  * filtering data using the Nashorn engine
  */
object FilterBuilder {

  def createJsonFilter(theJavaScript: String): Option[FilterJsonMsg] = Option(new FilterJsonMsg(theJavaScript))

}

package models

import play.api.libs.json.JsValue
import play.libs.Json

/**
  * a filter of json messages.
  * evaluate a javascript containing a "filter(data)" function
  */
class FilterJsonMsg(val clientScript: String) extends Filter(clientScript) {

  import Filter._

  // return true if the data has passed the filter or no filter function found,
  // else return false
  def accept(data: JsValue): Boolean = {
    invocable match {
      // if have no function return true
      case null => true
      case ok =>
        try {
          // invoke the "filter(data)" function of the clientScript
          invocable.invokeFunction("filter", data).asInstanceOf[Boolean]
        } catch {
          // return false on any errors
          case ex: Throwable => logger.info(s"-----> error in the filter function: \n $ex"); false
        }
    }
  }

}
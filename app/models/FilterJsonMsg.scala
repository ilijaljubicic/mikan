package models

import java.util.concurrent.Executors

import delight.nashornsandbox.NashornSandboxes
import play.api.libs.json.JsValue

import scala.util.{Failure, Success, Try}

/**
  * a filter of messages.
  * evaluate a javascript containing code to filter a json message called "message"
  * passed in as a string
  */
class FilterJsonMsg(val clientScript: String) {

  // see: https://github.com/javadelight/delight-nashorn-sandbox
  // The sandbox by default blocks access to all Java classes
  val sandbox = NashornSandboxes.create
  // limiting the CPU time of scripts
  sandbox.setMaxCPUTime(200)
  sandbox.setExecutor(Executors.newSingleThreadExecutor)
  // allow to pass the json message as a string
  sandbox.allow(classOf[String])

  val logger = org.slf4j.LoggerFactory.getLogger("models.FilterJsonMsg")

  // return true if the msg has passed the filter or has errors,
  // else return false
  def accept(msg: JsValue): Boolean = {
    // inject the msg, the filter must use the "msg" variable provided as a string
    sandbox.inject("msg", msg.toString)
    // run the script with the given msg
    Try(sandbox.eval(clientScript)) match {
      case Success(result) =>
        logger.info(s"-----> script: $result ")
        if (result.isInstanceOf[Boolean]) result.asInstanceOf[Boolean] else true

      case Failure(x) =>
        logger.info(s"-----> could not evaluate script: $x ")
        true
    }
  }

}

package models

import java.util.concurrent.Executors
import delight.nashornsandbox.NashornSandboxes
import play.api.Play
import play.api.libs.json.JsValue
import scala.util.{Failure, Success, Try}

/**
  * a filter of messages.
  * evaluate a string containing javascript code to filter a json message called "msg"
  * injected in as a string
  *
  * ref: https://github.com/javadelight/delight-nashorn-sandbox
  */
class FilterJsonMsg (val clientScript: String) {

  // get the max cpu time the script can use from application.conf
  val cpuTime = Play.current.configuration.getLong("mikan.filter.cputime").getOrElse(200L)

  // the sandbox by default blocks access to all Java classes
  val sandbox = NashornSandboxes.create
  // limiting the CPU time of scripts
  sandbox.setMaxCPUTime(cpuTime)
  sandbox.setExecutor(Executors.newSingleThreadExecutor)
  // allow to pass the json message as a string
  sandbox.allow(classOf[String])

  val logger = org.slf4j.LoggerFactory.getLogger("models.FilterJsonMsg")

  // return true if the msg has passed the filter or has errors,
  // else return false
  def accept(msg: JsValue): Boolean = {
    // inject the msg, the filter must use the name "mikanMsg" provided as a string
    // to refer to the message inside the script
    sandbox.inject("mikanMsg", msg.toString)
    // run the script with the given msg
    Try(sandbox.eval(clientScript)) match {
      case Success(result) =>
        logger.info(s" script: $result ")
        if (result.isInstanceOf[Boolean]) result.asInstanceOf[Boolean] else true

      case Failure(x) =>
        logger.info(s" could not evaluate script: $x ")
        true
    }
  }

}

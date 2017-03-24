package models

import javax.script.{Invocable, ScriptEngineManager}
import play.api.libs.json.JsValue
import scala.util.{Failure, Success, Try}
//import delight.nashornsandbox.NashornSandboxes
import java.util.concurrent.Executors

/**
  * filtering data using the Nashorn engine
  */
object Filter {
  // see: https://github.com/javadelight/delight-nashorn-sandbox
  // The sandbox by default blocks access to all Java classes
  // val sandbox = NashornSandboxes.create
  // limiting the CPU time of scripts
  // sandbox.setMaxCPUTime(1000)
  // sandbox.setExecutor(Executors.newSingleThreadExecutor)
  // sandbox.allow(classOf[someJavaClass])

  def createJsonFilter(theJavaScript: String): Option[FilterJsonMsg] = {
    val logger = org.slf4j.LoggerFactory.getLogger("models.Filter")
    val engine = new ScriptEngineManager().getEngineByName("nashorn")
    val invocable = engine.asInstanceOf[Invocable]

    // evaluate the javascript
    Try(engine.eval(theJavaScript)) match {
      case Success(x) =>
        logger.info(s"-----> script: \n $x ")
        Option(new FilterJsonMsg(theJavaScript, invocable))

      case Failure(x) =>
        logger.info(s"-----> could not evaluate script: \n $x ")
        None
    }
  }
}

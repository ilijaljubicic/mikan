package models

import javax.script.{Invocable, ScriptEngineManager}
import play.api.libs.json.JsValue
import scala.util.{Failure, Success, Try}

/**
  * filtering data using the Nashorn engine
  */
object Filter {
  // the JavaScript engine
  val engine = new ScriptEngineManager().getEngineByName("nashorn")
  //      val bindings = engine.createBindings()
  //      bindings.put("ocr", ocr)
  //      bindings.put("vision", vision)
  val invocable = engine.asInstanceOf[Invocable]
}

/**
  * filter data messages by evaluating a JavaScript function
  */
abstract class Filter(val theJavaScript: String) {

  import Filter._

  protected val logger = org.slf4j.LoggerFactory.getLogger("models.Filter")

  // evaluate the javascript
  Try(engine.eval(theJavaScript)) match {
    case Success(x) => logger.info(s"-----> script: \n $x ")
    case Failure(x) => logger.info(s"-----> could not evaluate script: \n $x ")
  }

  // return true if the data is accepted else return false
  def accept(data: JsValue): Boolean
}

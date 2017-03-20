package controllers

import models.FilterJsonMsg
import play.api.libs.json.Json


/**
  *
  */
object TestApp {

  def main(args: Array[String]) {

    testNashorn()

  }

  def testNashorn() = {
    println("....testNashorn start...\n")

    //    def loadScript(name: String): Try[String] = Try(Source.fromFile(name).mkString)
    //
    //    //  "/Users/ringo/cesium/czml-server/akka-server/cesiumViewer.js"
    //    loadScript("xx") match {
    //      case Success(scriptx) => engine.eval(scriptx)
    //      case Failure(_) => println("did not load script ")
    //    }

    val msg = Json.obj("entity" -> "testEntity", "milsymbol" -> "SUG-UCI----D", "lat" -> 12.34, "lon" -> 56.78)

  //  val msg = """{"entity": "testEntity", "milsymbol": "SUG-UCI----D", "lat": 12.34, "lon": 56.78 }""".stripMargin

    //      val script = """ load ('scripts/dis7.js')
    //                         var pduFactory = new dis.PduFactory();
    //                         function filter(data) {
    //                          var pdu = pduFactory.createPdu(data);
    //                          var espdu = new dis.EntityStatePdu();
    //                          if(espdu.pduType != 1) return false;
    //                          return true;
    //                        } """.stripMargin

    val testScript =
      """ function filter(data) {
           var obj = JSON.parse(data);
           if(obj.lat > 10 && obj.lat < 20) return true;
           return false;
         } """.stripMargin

    println("\n=======> testNashorn accept: " + new FilterJsonMsg(testScript).accept(msg))


    println("\n....testNashorn finish...")
  }

}

package messages

import play.api.libs.json._


/**
  * the Mikan protocol
  */
package object Mikan {

  sealed trait MikanMsg {
    val mikanType: String
  }

  // {"mikanType": "filter", "script": codeString}
  case class MikanFilter(script: String) extends MikanMsg {
    val mikanType = MikanFilter.mikanType
  }

  object MikanFilter {
    val mikanType = "filter"

    val fmtx = Json.format[MikanFilter]

    val theReads = new Reads[MikanFilter] {
      def reads(js: JsValue): JsResult[MikanFilter] = {
        if ((js \ "mikanType").asOpt[String].contains(mikanType)) {
          fmtx.reads(js)
        } else {
          JsError(s"Error reading message: $js")
        }
      }
    }

    val theWrites = new Writes[MikanFilter] {
      def writes(c: MikanFilter) = Json.obj("mikanType" -> mikanType) ++ fmtx.writes(c)
    }

    implicit val fmt = Format(theReads, theWrites)
  }

  // {"mikanType": "subscribe", "topic": topicName}
  case class MikanSubscribe(topic: String) extends MikanMsg {
    val mikanType = MikanSubscribe.mikanType
  }

  object MikanSubscribe {
    val mikanType = "subscribe"

    val fmtx = Json.format[MikanSubscribe]

    val theReads = new Reads[MikanSubscribe] {
      def reads(js: JsValue): JsResult[MikanSubscribe] = {
        if ((js \ "mikanType").asOpt[String].contains(mikanType)) {
          fmtx.reads(js)
        } else {
          JsError(s"Error reading message: $js")
        }
      }
    }

    val theWrites = new Writes[MikanSubscribe] {
      def writes(c: MikanSubscribe) = Json.obj("mikanType" -> mikanType) ++ fmtx.writes(c)
    }

    implicit val fmt = Format(theReads, theWrites)
  }

  // {"mikanType": "publish", "topic": topicName}
  case class MikanPublish(topic: String) extends MikanMsg {
    val mikanType = MikanPublish.mikanType
  }

  object MikanPublish {
    val mikanType = "publish"

    val fmtx = Json.format[MikanPublish]

    val theReads = new Reads[MikanPublish] {
      def reads(js: JsValue): JsResult[MikanPublish] = {
        if ((js \ "mikanType").asOpt[String].contains(mikanType)) {
          fmtx.reads(js)
        } else {
          JsError(s"Error reading message: $js")
        }
      }
    }

    val theWrites = new Writes[MikanPublish] {
      def writes(c: MikanPublish) = Json.obj("mikanType" -> mikanType) ++ fmtx.writes(c)
    }

    implicit val fmt = Format(theReads, theWrites)
  }

  object MikanMsg {

    val theReads = new Reads[MikanMsg] {
      def reads(json: JsValue): JsResult[MikanMsg] = {
        (json \ "mikanType").asOpt[String].map({
          case "filter" => MikanFilter.fmt.reads(json)
          case "subscribe" => MikanSubscribe.fmt.reads(json)
          case "publish" => MikanPublish.fmt.reads(json)
          case err => JsError(s"Error unknown mikanType: $err")
        }).getOrElse(JsError("Error no mikanType"))
      }
    }

    val theWrites = new Writes[MikanMsg] {
      def writes(msg: MikanMsg) = {
        msg match {
          case s: MikanFilter => MikanFilter.fmt.writes(s)
          case s: MikanSubscribe => MikanSubscribe.fmt.writes(s)
          case s: MikanPublish => MikanPublish.fmt.writes(s)
          case _ => JsNull
        }
      }
    }

    implicit val fmt: Format[MikanMsg] = Format(theReads, theWrites)

  }

}
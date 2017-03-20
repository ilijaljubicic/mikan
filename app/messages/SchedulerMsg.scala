package messages

/**
  * messages used to control asynchronous tasks
  */
object SchedulerMsg {

    case object Start

    case object Stop

    case object Resume

    case object Tick

    case object Done

}

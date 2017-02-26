class ScheduledOrderSynchronizer extends Actor {

  private val SYNC_ALL_ORDERS = "SYNC_ALL_ORDERS"

  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    scheduler = context.system.scheduler.schedule(
      initialDelay = 10 seconds,
      interval = 5 minutes,
      receiver = self,
      message = SYNC_ALL_ORDERS
    )
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case SYNC_ALL_ORDERS =>
      try {
        // synchronize all the orders
      } catch {
        case t: Throwable =>
          // report errors
      }
  }

}

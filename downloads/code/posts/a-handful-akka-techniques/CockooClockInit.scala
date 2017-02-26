object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Akka.system.actorOf(Props(new ScheduledOrderSynchronizer), name = "orderSynchronizer")
  }

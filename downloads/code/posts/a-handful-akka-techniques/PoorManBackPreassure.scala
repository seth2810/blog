import akka.actor.{ Props, Actor }
import akka.event.Logging
import akka.routing.RoundRobinRouter
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

abstract class Processor(dataSetId: Long) extends Actor {

  val log = Logging(context.system, "application")

  val workers = context.actorOf(Props[ItemProcessingWorker].withRouter(RoundRobinRouter(100)))
  val producer = context.actorOf(Props[DataProducer], name = "dataProducer")

  var totalItemCount = -1
  var currentItemCount = 0

  var allProcessedItemsCount = 0
  var allProcessingErrors: List[ItemProcessingError] = List.empty

  val MAX_LOAD = 50

  def receive = {

    case Process =>
      if (totalItemCount == -1) {
        totalItemCount = totalItems
        log.info(s"Starting to process set with ID $dataSetId, we have $totalItemCount items to go through")
      }
      val index = allProcessedItemsCount + allProcessingErrors.size
      if (currentItemCount < MAX_LOAD) {
        producer ! FetchData(index)
      }

    case Data(items) =>
      processBatch(items)

    case GetLoad =>
      sender ! currentItemCount

    case ProcessedOneItem =>
      allProcessedItemsCount = allProcessedItemsCount + 1
      currentItemCount = currentItemCount - 1
      continueProcessing()

    case error @ ItemProcessingError(_, _, _) =>
      allProcessingErrors = error :: allProcessingErrors
      currentItemCount = currentItemCount - 1
      continueProcessing()

  }

  def processBatch(batch: List[Item]) = {

    if (batch.isEmpty) {
      log.info(s"Done migrating all items for data set $dataSetId. $totalItems processed items, we had ${allProcessingErrors.size} errors in total")
    } else {
      // distribute the work
      batch foreach { item =>
        workers ! item
        currentItemCount = currentItemCount + 1
      }
    }

  }

  def continueProcessing() = {

    val itemsProcessed = allProcessedItemsCount + allProcessingErrors.size

    if (itemsProcessed > 0 && itemsProcessed % 100 == 0) {
      log.info(s"Processed $itemsProcessed out of $totalItems with ${allProcessingErrors.size} errors")
    }

    self ! Process

  }

  def totalItems: Int

}

abstract class DataProducer extends Actor {

  private val MAX_LOAD = 50

  def receive = {

    case FetchData(currentIndex) =>
      throttleDown()
      sender ! Data(fetchBatch(currentIndex))

  }

  def throttleDown(): Unit = {
    implicit val timeout = Timeout(5.seconds)
    val eventuallyLoad = context.parent ? GetLoad
    try {
      val load = Await.result(eventuallyLoad, 5.seconds)

      if (load.asInstanceOf[Int] > MAX_LOAD) {
        Thread.sleep(5000)
        throttleDown()
      }

    } catch {
      case t: Throwable =>
        // we most likely have timed out - wait a bit longer
        throttleDown()
    }
  }

  def fetchBatch(currentIndex: Int): List[Item]

}

abstract class ItemProcessingWorker extends Actor {

  def receive = {
    case ProcessItem(item) =>
      try {
        process(item) match {
          case None => sender ! ProcessedOneItem
          case Some(error) => sender ! error
        }
      } catch {
        case t: Throwable =>
          sender ! ItemProcessingError(item.id, "Unhandled error", Some(t))
      }
  }

  def process(item: Item): Option[ItemProcessingError]

}

case object Process
trait Item {
  val id: Int
}
case class FetchData(currentIndex: Int)
case class Data(items: List[Item])
case object GetLoad
case class ProcessItem(item: Item)
case object ProcessedOneItem
case class ItemProcessingError(itemId: Int, message: String, error: Option[Throwable])

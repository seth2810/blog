import akka.actor.{ Props, Actor }
import akka.event.Logging
import akka.routing.RoundRobinRouter

abstract class BatchProcessor(dataSetId: Long) extends Actor {

  val log = Logging(context.system, "application")

  val workers = context.actorOf(Props[ItemProcessingWorker].withRouter(RoundRobinRouter(100)))

  var totalItemCount = -1
  var currentBatchSize: Int = 0
  var currentProcessedItemsCount: Int = 0
  var currentProcessingErrors: List[ItemProcessingError] = List.empty

  var allProcessedItemsCount = 0
  var allProcessingErrors: List[ItemProcessingError] = List.empty

  def receive = {

    case ProcessBatch =>
      if (totalItemCount == -1) {
        totalItemCount = totalItems
        log.info(s"Starting to process set with ID $dataSetId, we have $totalItemCount items to go through")
      }
      val batch = fetchBatch
      processBatch(batch)

    case ProcessedOneItem =>
      currentProcessedItemsCount = currentProcessedItemsCount + 1
      continueProcessing()

    case error @ ItemProcessingError(_, _, _) =>
      currentProcessingErrors = error :: currentProcessingErrors
      continueProcessing()

  }

  def processBatch(batch: List[BatchItem]) = {

    if (batch.isEmpty) {
      log.info(s"Done migrating all items for data set $dataSetId. $totalItems processed items, we had ${allProcessingErrors.size} errors in total")
    } else {
      // reset processing state for the current batch
      currentBatchSize = batch.size
      allProcessedItemsCount = currentProcessedItemsCount + allProcessedItemsCount
      currentProcessedItemsCount = 0
      allProcessingErrors = currentProcessingErrors ::: allProcessingErrors
      currentProcessingErrors = List.empty

      // distribute the work
      batch foreach { item =>
        workers ! item
      }
    }

  }

  def continueProcessing() = {

    val itemsProcessed = currentProcessedItemsCount + currentProcessingErrors.size

    if (itemsProcessed > 0 && itemsProcessed % 100 == 0) {
      log.info(s"Processed $itemsProcessed out of $currentBatchSize with ${currentProcessingErrors.size} errors")
    }

    if (itemsProcessed == currentBatchSize) {
      self ! ProcessBatch
    }

  }

  def totalItems: Int

  def fetchBatch: List[BatchItem]

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

  def process(item: BatchItem): Option[ItemProcessingError]

}

case object ProcessBatch
trait BatchItem {
  val id: Int
}
case class ProcessItem(item: BatchItem)
case object ProcessedOneItem
case class ItemProcessingError(itemId: Int, message: String, error: Option[Throwable])

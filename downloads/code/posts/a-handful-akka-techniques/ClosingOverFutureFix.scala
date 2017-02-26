def receive = {
  case ComputeResult(itemId: Long) =>
    val originalSender = sender
    computeResult(itemId).map { result =>
      originalSender ! result
    }
}    

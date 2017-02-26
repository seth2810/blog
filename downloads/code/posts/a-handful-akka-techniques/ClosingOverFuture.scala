def receive = {
  case ComputeResult(itemId: Long) =>
    computeResult(itemId).map { result =>
      // gotcha!
      sender ! result
    }
}

def computeResult(itemId: Long): Future[Int] = ???

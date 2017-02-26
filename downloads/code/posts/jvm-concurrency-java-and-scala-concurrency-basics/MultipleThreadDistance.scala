class Matcher(words: Array[String]) {

  def bestMatch(targetText: String) = {

    val limit = targetText.length
    val v0 = new Array[Int](limit + 1)
    val v1 = new Array[Int](limit + 1)

    def editDistance(word: String, v0: Array[Int], v1: Array[Int]) = {
      ...
    }

    @tailrec
    /** Scan all known words in range to find best match.
      *
      * @param index next word index
      * @param bestDist minimum distance found so far
      * @param bestMatch unique word at minimum distance, or None if not unique
      * @return best match
      */
    def best(index: Int, bestDist: Int, bestMatch: Option[String]): DistancePair =
      if (index < words.length) {
        val newDist = editDistance(words(index), v0, v1)
        val next = index + 1
        if (newDist < bestDist) best(next, newDist, Some(words(index)))
        else if (newDist == bestDist) best(next, bestDist, None)
        else best(next, bestDist, bestMatch)
      } else DistancePair(bestDist, bestMatch)

    best(0, Int.MaxValue, None)
  }
}

class ParallelCollectionDistance(words: Array[String], size: Int) extends TimingTestBase {

  val matchers = words.grouped(size).map(l => new Matcher(l)).toList

  def shutdown = {}

  def blockSize = size

  /** Find best result across all matchers, using parallel collection. */
  def bestMatch(target: String) = {
    matchers.par.map(m => m.bestMatch(target)).
      foldLeft(DistancePair.worstMatch)((a, m) => DistancePair.best(a, m))
  }
}

class DirectBlockingDistance(words: Array[String], size: Int) extends TimingTestBase {

  val matchers = words.grouped(size).map(l => new Matcher(l)).toList

  def shutdown = {}

  def blockSize = size

  /** Find best result across all matchers, using direct blocking waits. */
  def bestMatch(target: String) = {
    import ExecutionContext.Implicits.global
    val futures = matchers.map(m => future { m.bestMatch(target) })
    futures.foldLeft(DistancePair.worstMatch)((a, v) =>
      DistancePair.best(a, Await.result(v, Duration.Inf)))
  }
}
val limit = targetText.length
/** Calculate edit distance from targetText to known word.
  *
  * @param word known word
  * @param v0 int array of length targetText.length + 1
  * @param v1 int array of length targetText.length + 1
  * @return distance
  */
def editDistance(word: String, v0: Array[Int], v1: Array[Int]) = {

  val length = word.length

  @tailrec
  def distanceByRow(rnum: Int, r0: Array[Int], r1: Array[Int]): Int = {
    if (rnum >= length) r0(limit)
    else {

      // first element of r1 = delete (i+1) chars from target to match empty 'word'
      r1(0) = rnum + 1

      // use formula to fill in the rest of the row
      for (j <- 0 until limit) {
        val cost = if (word(rnum) == targetText(j)) 0 else 1
        r1(j + 1) = min(r1(j) + 1, r0(j + 1) + 1, r0(j) + cost);
      }

      // recurse with arrays swapped for next row
      distanceByRow(rnum + 1, r1, r0)
    }
  }

  // initialize v0 (prior row of distances) as edit distance for empty 'word'
  for (i <- 0 to limit) v0(i) = i

  // recursively process rows matching characters in word being compared to find best
  distanceByRow(0, v0, v1)
}
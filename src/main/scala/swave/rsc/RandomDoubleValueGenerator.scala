package swave.rsc

// simple XORshift* random number generator (see, e.g., http://en.wikipedia.org/wiki/Xorshift)
class RandomLongValueGenerator(seed: Long = 182642182642182642L) extends Iterator[Long] {
  private[this] var state = seed

  def hasNext = true

  def next(): Long = {
    var x = state
    x ^= x << 21
    x ^= x >>> 35
    x ^= x << 4
    state = x
    (x * 0x2545f4914f6cdd1dL) - 1
  }
}

class RandomDoubleValueGenerator(seed: Long = 182642182642182642L) extends Iterator[Double] {
  private[this] val inner = new RandomLongValueGenerator(seed)

  def hasNext = true

  def next(): Double = (inner.next() & ((1L << 53) - 1)) / (1L << 53).toDouble
}

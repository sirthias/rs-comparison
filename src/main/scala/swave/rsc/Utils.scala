package swave.rsc

import scala.collection.immutable

object Utils {

  def iterable[T](iter: ⇒ Iterator[T]): immutable.Iterable[T] =
    new immutable.Iterable[T] { def iterator = iter }
}

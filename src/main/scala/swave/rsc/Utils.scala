package swave.rsc

object Utils {

  def iterable[T](iter: ⇒ Iterator[T]): Iterable[T] =
    new Iterable[T] { def iterator = iter }
}

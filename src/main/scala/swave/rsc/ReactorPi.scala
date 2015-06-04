package swave.rsc

import java.util.concurrent.Executors
import reactor.Environment
import reactor.core.processor.RingBufferProcessor
import reactor.core.support.NamedDaemonThreadFactory
import reactor.rx._
import scala.io.StdIn
import scala.collection.JavaConverters._

object ReactorPi extends App {
  import ReactorCompat._

  Environment.initialize()

  val dispatcher1 = Environment.sharedDispatcher()

  val pool = Executors.newCachedThreadPool(new NamedDaemonThreadFactory("tee", null, null, true))

  val ints: Stream[Int] =
    Streams.from(Iterable.tabulate(10000)(identity).asJava)
      .dispatchOn(dispatcher1).subscribeOn(dispatcher1)
      .log("A")
      .process(RingBufferProcessor.create[Int](pool, 8))

  val evens: Stream[Int] =
    ints
      .filter { (i: Int) ⇒ (i & 1) == 0 }
      .log("B")

  val odds: Stream[Int] =
    ints
      .filter { (i: Int) ⇒ (i & 1) == 1 }
      .log("C")

  Streams.merge(evens, odds)
    .log("D")
    .take(1000)
    .consume()
    .start()

  StdIn.readLine()
  Environment.terminate()
}

object ReactorCompat {
  implicit def scalaFunction2reactorFunction[A, B](f: A ⇒ B): reactor.fn.Function[A, B] =
    new reactor.fn.Function[A, B] {
      override def apply(a: A) = f(a)
    }

  implicit def scalaFunction2reactorBifunction[A, B, C](f: (A, B) ⇒ C): reactor.fn.BiFunction[A, B, C] =
    new reactor.fn.BiFunction[A, B, C] {
      override def apply(a: A, b: B) = f(a, b)
    }

  implicit def scalaFunction2reactorPredicate[A](f: A ⇒ Boolean): reactor.fn.Predicate[A] =
    new reactor.fn.Predicate[A] {
      def test(t: A) = f(t)
    }

  implicit def scalaFunction2reactorConsumer[A](f: A ⇒ Unit): reactor.fn.Consumer[A] =
    new reactor.fn.Consumer[A] {
      def accept(t: A) = f(t)
    }
}
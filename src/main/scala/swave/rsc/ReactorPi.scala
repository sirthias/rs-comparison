package swave.rsc

import scala.io.StdIn
import java.util.concurrent.Executors
import java.util.{ List ⇒ JList }
import reactor.Environment
import reactor.core.processor.{ RingBufferWorkProcessor, RingBufferProcessor }
import reactor.core.support.NamedDaemonThreadFactory
import reactor.rx._
import scala.collection.JavaConverters._
import Model._
import Utils._

object ReactorPi extends App {
  import ReactorCompat._

  Environment.initialize()

  val dispatcher1 = Environment.sharedDispatcher()
  //  val dispatcher2 = Environment.cachedDispatcher()
  //  val dispatcher3 = Environment.cachedDispatcher()
  //  val dispatcher4 = Environment.cachedDispatcher()
  //  val dispatcher5 = Environment.cachedDispatcher()

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

  //  val points: Stream[Point] =
  //    Streams.from(iterable(new RandomDoubleValueGenerator()).asJava)
  //      .dispatchOn(dispatcher1).subscribeOn(dispatcher1)
  //      .buffer(2)
  //      .map[Point] { (list: JList[Double]) ⇒ Point(list.get(0), list.get(1)) }
  //      //.log("points")
  //      .broadcast()
  //
  //  val innerSamples: Stream[Sample] =
  //    points.dispatchOn(dispatcher2).subscribeOn(dispatcher2)
  //      //.log("inner-1")
  //      .filter { (p: Point) ⇒ p.isInner }
  //      .map[Sample](InnerSample(_: Point))
  //  //.broadcast()
  //  //.log("inner-2")
  //
  //  val outerSamples: Stream[Sample] =
  //    points.dispatchOn(dispatcher3).subscribeOn(dispatcher3)
  //      //.log("outer-1")
  //      .filter { (p: Point) ⇒ !p.isInner }
  //      .map[Sample](OuterSample(_: Point))
  //  //.broadcast()
  //  //.log("outer-2")
  //
  //  //val simulationStates: Stream[SimulationState] =
  //  Streams.merge(innerSamples, outerSamples)
  //    //.dispatchOn(dispatcher4).subscribeOn(dispatcher4)
  //    //.log("D")
  //    .scan(SimulationState(0, 0), { (_: SimulationState).withNextSample(_: Sample) })
  //
  //    //simulationStates.dispatchOn(dispatcher5).subscribeOn(dispatcher5)
  //    //.throttle(1000)
  //    //.map[String] { (ss: SimulationState) ⇒ f"After ${ss.totalSamples}%8d samples π is approximated as ${ss.π}%.5f" }
  //    .log("RESULT")
  //    .take(10000)
  //    .consume()
  //    .start()

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
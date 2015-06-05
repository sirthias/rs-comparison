package swave.rsc

import scala.io.StdIn
import java.util.concurrent.{ TimeUnit, Executors }
import java.util.{ List ⇒ JList }
import reactor.Environment
import reactor.core.processor.RingBufferProcessor
import reactor.core.support.NamedDaemonThreadFactory
import reactor.rx._
import Model._
import Utils._

object ReactorPi extends App {
  import ReactorCompat._

  Environment.initialize()

  val pool = Executors.newCachedThreadPool(new NamedDaemonThreadFactory("tee", null, null, true))

  val points: Stream[Point] =
    Streams.wrap(SynchronousIterablePublisher(iterable(new RandomDoubleValueGenerator()), "DoublePub"))
      .dispatchOn(Environment.sharedDispatcher())
      .buffer(2)
      .map[Point] { (list: JList[Double]) ⇒ Point(list.get(0), list.get(1)) }
      .log("points")
      .process(RingBufferProcessor.create[Point](pool, 32))

  val innerSamples: Stream[Sample] =
    points
      .log("inner-1")
      .filter { (p: Point) ⇒ p.isInner }
      .map[Sample](InnerSample(_: Point))
      .log("inner-2")

  val outerSamples: Stream[Sample] =
    points
      .log("outer-1")
      .filter { (p: Point) ⇒ !p.isInner }
      .map[Sample](OuterSample(_: Point))
      .log("outer-2")

  Streams.merge(innerSamples, outerSamples)
    .dispatchOn(Environment.cachedDispatcher())
    .scan(SimulationState(0, 0), { (_: SimulationState).withNextSample(_: Sample) })
    .log("result")
    //.sample(1, TimeUnit.SECONDS)
    .map[String] { (ss: SimulationState) ⇒ f"After ${ss.totalSamples}%8d samples π is approximated as ${ss.π}%.5f" }
    .take(10000)
    .consume()
    //.consume(println(_: String))
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
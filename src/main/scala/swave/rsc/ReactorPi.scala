package swave.rsc

import java.util.{ List ⇒ JList }
import java.util.concurrent.{ CountDownLatch, TimeUnit, Executors }
import reactor.Environment
import reactor.core.processor.RingBufferProcessor
import reactor.core.support.NamedDaemonThreadFactory
import reactor.rx.action.Control
import reactor.rx._
import Model._
import Utils._

object ReactorPi extends App {
  import ReactorCompat._

  Environment.initialize()

  val pool = Executors.newCachedThreadPool(new NamedDaemonThreadFactory("tee", null, null, true))
  val done = new CountDownLatch(10)

  val points: Stream[Point] =
    // Streams.from(iterable(new RandomDoubleValueGenerator()))
    Streams.wrap(SynchronousIterablePublisher(iterable(new RandomDoubleValueGenerator()), "DoublePub"))
      .dispatchOn(Environment.cachedDispatcher())
      .buffer(2)
      .map[Point] { (list: JList[Double]) ⇒ Point(list.get(0), list.get(1)) }
  //.process(RingBufferProcessor.create[Point](pool, 32))

  val innerSamples: Stream[Sample] =
    points.dispatchOn(Environment.cachedDispatcher())
      .filter { (p: Point) ⇒ p.isInner }
      .map[Sample](InnerSample(_: Point))

  val outerSamples: Stream[Sample] =
    points.dispatchOn(Environment.cachedDispatcher())
      .filter { (p: Point) ⇒ !p.isInner }
      .map[Sample](OuterSample(_: Point))

  var control: Control =
    Streams.merge(innerSamples, outerSamples)
      .dispatchOn(Environment.cachedDispatcher())
      .scan(State(0, 0), { (_: State).withNextSample(_: Sample) })
      .sample(1, TimeUnit.SECONDS)
      //.take(10)
      .map[Unit] { (ss: State) ⇒
        println(f"After ${ss.totalSamples}%,10d samples π is approximated as ${ss.π}%.5f")
        done.countDown()
        if (done.getCount == 0) control.cancel()
      }
      .consumeLater()

  control.start()
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
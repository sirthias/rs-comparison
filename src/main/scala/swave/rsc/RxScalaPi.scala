package swave.rsc

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import rx.lang.scala.subjects.PublishSubject
import rx.lang.scala.schedulers.ExecutionContextScheduler
import rx.lang.scala.Observable
import Utils._
import Model._

object RxScalaPi extends App {
  val scheduler = ExecutionContextScheduler(ExecutionContext.global)

  val points = PublishSubject[Point]()
  val pointsFanout = points.onBackpressureDrop
  val done = new CountDownLatch(1)

  val sub =
    Observable.from(iterable(new RandomDoubleValueGenerator()))
      .observeOn(scheduler)
      .tumblingBuffer(2)
      .map { case x +: y +: Nil ⇒ Point(x, y) }
      .subscribe(points)

  val innerSamples =
    pointsFanout.observeOn(scheduler)
      .filter { _.isInner }
      .map(InnerSample)

  val outerSamples =
    pointsFanout.observeOn(scheduler)
      .filter { !_.isInner }
      .map(OuterSample)

  innerSamples
    .merge(outerSamples).observeOn(scheduler)
    .scan(State(0, 0)) { _ withNextSample _ }
    .throttleLast(1.second)
    .map(state ⇒ f"After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .finallyDo { done.countDown() }
    .foreach(println(_))

  done.await()
  sub.unsubscribe()
}


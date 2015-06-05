package swave.rsc

import scala.io.StdIn
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

  val sub =
    Observable.from(iterable(new RandomDoubleValueGenerator()))
      .observeOn(scheduler)
      .tumblingBuffer(2)
      .map { case x +: y +: Nil ⇒ Point(x, y) }
      .subscribe(points)

  val innerSamples =
    pointsFanout.observeOn(scheduler)
      .filter { (p: Point) ⇒ p.isInner }
      .map(InnerSample)

  val outerSamples =
    pointsFanout.observeOn(scheduler)
      .filter { (p: Point) ⇒ !p.isInner }
      .map(OuterSample)

  innerSamples
    .merge(outerSamples).observeOn(scheduler)
    .scan(SimulationState(0, 0)) { _ withNextSample _ }
    .throttleLast(1.second)
    .map(state ⇒ f"After ${state.totalSamples}%8d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .finallyDo { sub.unsubscribe() }
    .foreach(println(_))

  StdIn.readLine()
}


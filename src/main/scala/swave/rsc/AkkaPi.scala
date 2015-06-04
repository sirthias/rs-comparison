package swave.rsc

import scala.util.{ Try, Failure, Success }
import scala.concurrent.duration._
import akka.stream.stage.{ Context, PushStage }
import akka.actor.ActorSystem
import akka.stream.{ OperationAttributes, ActorFlowMaterializer }
import akka.stream.scaladsl._
import Model._

object AkkaPi extends App {
  implicit val system = ActorSystem("AkkaPi")
  implicit val materializer = ActorFlowMaterializer()

  Source(() ⇒ new RandomDoubleValueGenerator())
    .grouped(2)
    .map { case x +: y +: Nil ⇒ Point(x, y) }
    .via(broadcastFilterMerge)
    .scan(SimulationState(0, 0)) { _ withNextSample _ }
    .conflate(identity)(Keep.right)
    .via(onePerSecValve)
    .map(state ⇒ f"After ${state.totalSamples}%8d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .via(onTermination(_ ⇒ system.shutdown()))
    .runForeach(println)

  ///////////////////////////////////////////

  def broadcastFilterMerge: Flow[Point, Sample, Unit] =
    Flow() { implicit b ⇒
      import FlowGraph.Implicits._

      val broadcast = b.add(Broadcast[Point](2)) // split one upstream into 2 downstreams
      val filterInner = b.add(Flow[Point].filter(_.isInner).map(InnerSample))
      val filterOuter = b.add(Flow[Point].filter(!_.isInner).map(OuterSample))
      val merge = b.add(Merge[Sample](2)) // merge 2 upstreams into one downstream

      broadcast.out(0) ~> filterInner ~> merge.in(0)
      broadcast.out(1) ~> filterOuter ~> merge.in(1)

      (broadcast.in, merge.out)
    }

  def onePerSecValve: Flow[SimulationState, SimulationState, Unit] =
    Flow() { implicit b ⇒
      import FlowGraph.Implicits._

      val tickSource = b.add(Source(Duration.Zero, 1.second, Tick))
      val zip = b.add(Zip[SimulationState, Tick.type]().withAttributes(OperationAttributes.inputBuffer(1, 1)))
      val selectFirst = b.add(Flow[(SimulationState, Tick.type)].map(_._1))

      tickSource.outlet ~> zip.in1
      zip.out ~> selectFirst.inlet

      (zip.in0, selectFirst.outlet)
    }

  def onTermination[T, Mat](f: Try[Unit] ⇒ Unit): Flow[T, T, Unit] =
    Flow[T] transform { () ⇒
      new PushStage[T, T] {
        def onPush(element: T, ctx: Context[T]) = ctx.push(element)
        override def onUpstreamFinish(ctx: Context[T]) = {
          f(Success(()))
          super.onUpstreamFinish(ctx)
        }
        override def onUpstreamFailure(cause: Throwable, ctx: Context[T]) = {
          f(Failure(cause))
          ctx.fail(cause)
        }
      }
    }
}


package swave.rsc

import com.typesafe.config.{ ConfigFactory, Config }
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.{ OperationAttributes, ActorFlowMaterializer }
import akka.stream.scaladsl._
import Model._

object AkkaPi extends App {
  val config: Config = ConfigFactory.parseString("""
    akka.actor.default-dispatcher.throughput = 256
    akka.stream.materializer.max-input-buffer-size = 256""")
  implicit val system = ActorSystem("AkkaPi", config)
  implicit val materializer = ActorFlowMaterializer()

  Source(() ⇒ new RandomDoubleValueGenerator())
    .grouped(2)
    .map { case x +: y +: Nil ⇒ Point(x, y) }
    .via(broadcastFilterMerge)
    .scan(State(0, 0)) { _ withNextSample _ }
    .conflate(identity)(Keep.right)
    .via(onePerSecValve)
    .map(state ⇒ f"After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .map(println)
    .runWith(Sink.onComplete(_ ⇒ system.shutdown()))

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

  def onePerSecValve: Flow[State, State, Unit] =
    Flow() { implicit b ⇒
      import FlowGraph.Implicits._

      val zip = b.add(ZipWith[State, Tick.type, State](Keep.left)
        .withAttributes(OperationAttributes.inputBuffer(1, 1)))
      val dropOne = b.add(Flow[State].drop(1))

      Source(Duration.Zero, 1.second, Tick) ~> zip.in1
      zip.out ~> dropOne.inlet

      (zip.in0, dropOne.outlet)
    }
}


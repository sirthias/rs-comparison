package swave.rsc

object Model {
  case class Point(x: Double, y: Double) {
    def isInner: Boolean = x * x + y * y < 1.0
  }

  sealed trait Sample
  case class InnerSample(point: Point) extends Sample
  case class OuterSample(point: Point) extends Sample

  case class SimulationState(totalSamples: Long, inCircle: Long) {
    def π: Double = (inCircle.toDouble / totalSamples) * 4.0
    def withNextSample(sample: Sample) =
      SimulationState(totalSamples + 1, if (sample.isInstanceOf[InnerSample]) inCircle + 1 else inCircle)
  }

  case object Tick
}

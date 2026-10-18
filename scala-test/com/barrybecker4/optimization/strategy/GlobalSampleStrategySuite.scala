package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.strategy.hillclimbing.SeedSelection
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class GlobalSampleStrategySuite extends AnyFunSuite {

  test("diverse topK selects well-separated seeds on a flat landscape") {
    val rnd = new Random(42)
    val optimizee = new FlatLandscapeOptimizee()
    
    val initial = NumericParameterArray(
      IndexedSeq(
        DoubleParameter(value = 0.0, minValue = -10.0, maxValue = 10.0, name = "x"),
        DoubleParameter(value = 0.0, minValue = -10.0, maxValue = 10.0, name = "y")
      ),
      rnd = rnd
    )
    val gs = new GlobalSampleStrategy(optimizee)
    
    gs.setSamplingRate(100)

    val diverse = gs.doOptimizationTopK(
      initial,
      fitnessRange = 100.0,
      k = 2,
      SeedSelection.DiverseFromTopM(topM = 50, minSeparation = 0.15)
    )

    assert(diverse.size == 2)
    val xs = diverse.map(_.pa.get(0).getValue)
    val ys = diverse.map(_.pa.get(1).getValue)
    val spread = diverse.head.pa.distance(diverse(1).pa)
    assert(spread > 8.0, s"expected well-separated seeds, distance=$spread xs=$xs ys=$ys")
  }
}

package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArrayWithFitness}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SeedForkingSuite extends AnyFunSuite {

  private def arr(x: Double, rnd: Random) = NumericParameterArray(
    IndexedSeq(
      DoubleParameter(value = x, minValue = -10.0, maxValue = 10.0, name = "x"),
      DoubleParameter(value = -6.0, minValue = -10.0, maxValue = 10.0, name = "y")),
    rnd = rnd)

  test("salt depends on the parameter values and fork index, not on the Random instance") {
    val a = arr(7.0, new Random(1))
    assert(SeedForking.forkSalt(a, 1) == SeedForking.forkSalt(arr(7.0, new Random(999)), 1))
    assert(SeedForking.forkSalt(a, 1) != SeedForking.forkSalt(a, 2))
    assert(SeedForking.forkSalt(a, 1) != SeedForking.forkSalt(arr(7.5, new Random(1)), 1))
  }

  test("forks of equal seeds propose identical neighbors, and differ between fork indexes") {
    def neighbor(x: Double, idx: Int) = {
      val forked = SeedForking.fork(ParameterArrayWithFitness(arr(x, new Random(x.toLong + idx)), 1.0), idx)
      forked.pa.getRandomNeighbor(0.5).get(0).getValue
    }
    assert(neighbor(7.0, 1) == neighbor(7.0, 1))
    assert(neighbor(7.0, 1) != neighbor(7.0, 2))
  }

  test("fork keeps the parameter values and fitness") {
    val forked = SeedForking.fork(ParameterArrayWithFitness(arr(7.0, new Random(3)), 42.0), 1)
    assert(forked.fitness == 42.0)
    assert(forked.pa.get(0).getValue == 7.0 && forked.pa.get(1).getValue == -6.0)
  }
}

package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ParameterCrossoverSuite extends AnyFunSuite {

  private val rnd = new Random(2)

  test("numeric uniform crossover preserves bounds and size") {
    val a = new NumericParameterArray(
      Array(0.1, 0.9),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("p0", "p1"),
      rnd)
    val b = new NumericParameterArray(
      Array(0.8, 0.2),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("p0", "p1"),
      rnd)
    val c = ParameterCrossover.cross(a, b, rnd).asInstanceOf[NumericParameterArray]
    assert(c.size == 2)
    assert(c.get(0).getValue >= 0.0 && c.get(0).getValue <= 1.0)
    assert(c.get(1).getValue >= 0.0 && c.get(1).getValue <= 1.0)
  }

  test("permuted OX crossover preserves length") {
    val params1 = (0 until 5).map(i => new IntegerParameter(i, 0, 10, "p" + i))
    val params2 = params1.reverse
    val p1 = PermutedParameterArray(params1.toIndexedSeq, rnd)
    val p2 = PermutedParameterArray(params2.toIndexedSeq, rnd)
    val c = ParameterCrossover.cross(p1, p2, rnd).asInstanceOf[PermutedParameterArray]
    assert(c.size == 5)
  }
}

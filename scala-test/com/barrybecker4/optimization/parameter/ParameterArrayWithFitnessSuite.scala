package com.barrybecker4.optimization.parameter

import org.scalatest.funsuite.AnyFunSuite
import ParameterArrayWithFitness.given

import scala.util.Random

class ParameterArrayWithFitnessSuite extends AnyFunSuite {

  private val rnd = new Random(1)

  private def npa(v: Double): NumericParameterArray = {
    val min = Array(-1.0, -1.0)
    val max = Array(1.0, 1.0)
    new NumericParameterArray(Array(v, 0.0), min, max, Array("a", "b"), rnd)
  }

  test("Ordering orders by fitness ascending") {
    val a = ParameterArrayWithFitness(npa(0), fitness = 2.0)
    val b = ParameterArrayWithFitness(npa(0), fitness = 5.0)
    assert(Ordering[ParameterArrayWithFitness].compare(a, b) < 0)
    assert(Ordering[ParameterArrayWithFitness].compare(b, a) > 0)
    assert(Ordering[ParameterArrayWithFitness].compare(a, a) == 0)
  }
}

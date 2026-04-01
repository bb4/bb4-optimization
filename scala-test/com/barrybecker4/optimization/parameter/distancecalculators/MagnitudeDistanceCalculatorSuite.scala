package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class MagnitudeDistanceCalculatorSuite extends AnyFunSuite {

  private val calc = new MagnitudeDistanceCalculator()
  private val rnd = new Random(1)

  private def npa(values: Int*): NumericParameterArray = {
    val params = values.zipWithIndex.map { case (v, i) =>
      new IntegerParameter(v, -10, 10, "p" + i)
    }.toIndexedSeq
    new NumericParameterArray(params, rnd = rnd)
  }

  test("identical arrays have distance 0") {
    val a = npa(1, 2, 3)
    assertResult(0.0)(calc.calculateDistance(a, a))
  }

  test("distance is absolute difference of value sums") {
    val a = npa(1, 2, 3)
    val b = npa(0, 1, 3)
    assertResult(2.0)(calc.calculateDistance(a, b))
  }

  test("length mismatch uses sum of each side") {
    val a = npa(1, 2)
    val b = npa(1, 2, 3)
    assertResult(3.0)(calc.calculateDistance(a, b))
  }
}

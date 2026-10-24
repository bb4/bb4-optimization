// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class CrossWithSuite extends AnyFunSuite {

  private def permutation(order: Seq[Int], seed: Long = 1): PermutedParameterArray =
    PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, order.size - 1, "p" + i): Parameter), new Random(seed))

  private def valuesOf(pa: ParameterArray): Seq[Double] = (0 until pa.size).map(pa.get(_).getValue)

  test("by default an array is crossed the way ParameterCrossover crosses it") {
    for (seed <- 1L to 20L) {
      val (a, b) = (permutation(new Random(seed).shuffle(0 until 12)), permutation(new Random(seed + 50).shuffle(0 until 12)))
      assertResult(valuesOf(ParameterCrossover.cross(a, b, new Random(seed))), s"seed $seed")(valuesOf(a.crossWith(b, new Random(seed))))
    }
  }

  test("numeric arrays cross by default too") {
    val params = (v: Double) => IndexedSeq[Parameter](DoubleParameter(v, 0.0, 1.0, "x"), DoubleParameter(1.0 - v, 0.0, 1.0, "y"))
    val (a, b) = (NumericParameterArray(params(0.1), rnd = new Random(1)), NumericParameterArray(params(0.8), rnd = new Random(1)))
    assertResult(valuesOf(ParameterCrossover.cross(a, b, new Random(3))))(valuesOf(a.crossWith(b, new Random(3))))
  }

  test("a subclass can cross itself its own way") {
    class Reversing(values: IndexedSeq[Parameter]) extends PermutedParameterArray(values, new Random(1)) {
      override def crossWith(other: ParameterArray, rnd: Random): ParameterArray = new Reversing(values.reverse)
    }
    val a = new Reversing(permutation(0 until 5).params)
    assertResult(Seq(4.0, 3.0, 2.0, 1.0, 0.0))(valuesOf(a.crossWith(a, new Random(1))))
  }

  test("arrays of different types still cannot be crossed") {
    val numeric = NumericParameterArray(IndexedSeq[Parameter](DoubleParameter(0.5, 0.0, 1.0, "x")), rnd = new Random(1))
    intercept[IllegalArgumentException](permutation(0 until 3).crossWith(numeric, new Random(1)))
  }
}

package com.barrybecker4.optimization.parameter.redistribution

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite

class RedistributionSuite extends AnyFunSuite {

  private implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-9)

  test("BooleanRedistribution maps [0,1] into [0,1]") {
    val r = BooleanRedistribution(0.25)
    for (x <- Seq(0.0, 0.1, 0.25, 0.5, 0.9, 1.0)) {
      val y = r.getValue(x)
      assert(y >= 0.0 && y <= 1.0)
    }
  }

  test("DiscreteRedistribution uses normalized special indices") {
    val r = new DiscreteRedistribution(5, Array(0, 4), Array(0.2, 0.2))
    assert(r.getValue(0.0) === 0.0)
    assert(r.getValue(1.0) === 1.0)
  }

  test("UniformRedistribution with special values is invertible at endpoints") {
    val r = new UniformRedistribution(Array(0.2, 0.8), Array(0.3, 0.3))
    assert(r.getValue(0.0) === 0.0)
    assert(r.getValue(1.0) === 1.0)
    assert(r.getInverseFunctionValue(0.0) === 0.0)
    assert(r.getInverseFunctionValue(1.0) === 1.0)
  }

  test("GaussianRedistribution is monotonic and bounded") {
    val r = GaussianRedistribution(0.5, 0.15)
    assert(r.getValue(0.0) >= 0.0 && r.getValue(0.0) <= 1.0)
    assert(r.getValue(0.5) >= r.getValue(0.0))
    assert(r.getValue(1.0) >= r.getValue(0.5))
    assert(r.getValue(1.0) <= 1.0)
  }
}

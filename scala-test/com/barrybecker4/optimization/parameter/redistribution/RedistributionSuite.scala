package com.barrybecker4.optimization.parameter.redistribution

import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite

class RedistributionSuite extends AnyFunSuite {

  private implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-9)

  private def assertInUnitInterval(y: Double): Unit =
    assert(y >= 0.0 && y <= 1.0, s"value $y outside [0,1]")

  private def assertMonotonicNonDecreasing(r: RedistributionFunction, steps: Int = 20): Unit = {
    var prev = r.getValue(0.0)
    assertInUnitInterval(prev)
    for (i <- 1 to steps) {
      val x = i.toDouble / steps
      val y = r.getValue(x)
      assertInUnitInterval(y)
      assert(y + 1e-12 >= prev, s"not monotonic at x=$x: $y < $prev")
      prev = y
    }
  }

  test("BooleanRedistribution maps [0,1] into [0,1]") {
    val r = BooleanRedistribution(0.25)
    for (x <- Seq(0.0, 0.1, 0.25, 0.5, 0.9, 1.0)) {
      assertInUnitInterval(r.getValue(x))
    }
  }

  test("BooleanRedistribution skewed is monotonic and bounded") {
    // Discrete spikes are not smoothly invertible on a fine grid; check range + endpoints.
    val r = BooleanRedistribution(0.8)
    assertMonotonicNonDecreasing(r)
    assertInUnitInterval(r.getValue(0.0))
    assertInUnitInterval(r.getValue(1.0))
    assert(r.getInverseFunctionValue(0.0) === 0.0)
    assertInUnitInterval(r.getInverseFunctionValue(1.0))
  }

  test("DiscreteRedistribution uses normalized special indices") {
    val r = new DiscreteRedistribution(5, Array(0, 4), Array(0.2, 0.2))
    assert(r.getValue(0.0) === 0.0)
    assert(r.getValue(1.0) === 1.0)
  }

  test("DiscreteRedistribution with special-prob sum 1 and last special below max constructs") {
    // Documented failure mode: when sum == 1 and last special < max index,
    // UniformRedistribution's final CDF point at x=1 can be nudged by MathUtil.EPS
    // without advancing a Double already at 1.0, failing PiecewiseFunction's
    // strictly-increasing x check (bb4-optimization / bb4-math 2.1).
    val r = new DiscreteRedistribution(10, Array(1, 2, 6), Array(0.3, 0.3, 0.4))
    assertMonotonicNonDecreasing(r)
    assertInUnitInterval(r.getValue(0.0))
    assertInUnitInterval(r.getValue(1.0))
  }

  test("UniformRedistribution with special values is invertible at endpoints") {
    val r = new UniformRedistribution(Array(0.2, 0.8), Array(0.3, 0.3))
    assert(r.getValue(0.0) === 0.0)
    assert(r.getValue(1.0) === 1.0)
    assert(r.getInverseFunctionValue(0.0) === 0.0)
    assert(r.getInverseFunctionValue(1.0) === 1.0)
  }

  test("UniformRedistribution with special-prob sum 1 constructs") {
    val r = new UniformRedistribution(Array(0.2, 0.8), Array(0.4, 0.6))
    assertMonotonicNonDecreasing(r)
    assertInUnitInterval(r.getValue(0.0))
    assertInUnitInterval(r.getValue(1.0))
  }

  test("GaussianRedistribution is monotonic and bounded") {
    val r = GaussianRedistribution(0.5, 0.15)
    assert(r.getValue(0.0) >= 0.0 && r.getValue(0.0) <= 1.0)
    assert(r.getValue(0.5) >= r.getValue(0.0))
    assert(r.getValue(1.0) >= r.getValue(0.5))
    assert(r.getValue(1.0) <= 1.0)
  }

  test("GaussianRedistribution narrow sigma constructs and is monotonic") {
    // Narrow σ makes the unit-interval CDF nearly a step; full inverse-forward on a
    // uniform y-grid is inherently poor. Construction + monotonic forward map is the
    // regression that previously failed (GAUSSIAN_NARROWEST / FunctionInverter).
    val r = GaussianRedistribution(0.5, 0.01)
    assertMonotonicNonDecreasing(r, steps = 40)
    assertInUnitInterval(r.getValue(0.0))
    assertInUnitInterval(r.getValue(0.5))
    assertInUnitInterval(r.getValue(1.0))
    // Forward-inverse near the mean stays usable for setValue/getValue round-trips.
    for (x <- Seq(0.4, 0.45, 0.5, 0.55, 0.6)) {
      val y = r.getValue(x)
      val xBack = r.getInverseFunctionValue(y)
      assert(math.abs(xBack - x) <= 0.1, s"forward-inverse |$xBack - $x| at x=$x y=$y")
    }
  }

  test("GaussianRedistribution skewed narrow constructs and stays bounded") {
    val r = GaussianRedistribution(0.9, 0.05)
    assertMonotonicNonDecreasing(r)
  }
}

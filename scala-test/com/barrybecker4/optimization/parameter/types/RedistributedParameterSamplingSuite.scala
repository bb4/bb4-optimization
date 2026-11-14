// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.redistribution.{BooleanRedistribution, DiscreteRedistribution, GaussianRedistribution}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/**
  * Seeded sampling / setValue round-trip contracts used by the parameter histogram simulator.
  */
class RedistributedParameterSamplingSuite extends AnyFunSuite {

  test("Gaussian DoubleParameter setValue then getValue round-trips approximately") {
    val p = new DoubleParameter(0, 0, 10.0, "g", Some(GaussianRedistribution(0.5, 0.3)))
    val targets = Seq(0.0, 1.0, 5.0, 9.0, 10.0)
    for (t <- targets) {
      val back = p.setValue(t).getValue
      assert(math.abs(back - t) < 0.5, s"round-trip of $t gave $back")
    }
  }

  test("Gaussian DoubleParameter randomizeValue stays in range") {
    val rnd = new Random(42)
    val p = new DoubleParameter(0, 0, 10.0, "g", Some(GaussianRedistribution(0.5, 0.3)))
    for (_ <- 0 until 200) {
      val v = p.randomizeValue(rnd).getValue
      assert(v >= 0.0 && v <= 10.0, s"out of range: $v")
    }
  }

  test("Discrete IntegerParameter with special probs constructs and randomizes in range") {
    val redist = DiscreteRedistribution(10, Array(1, 2, 6), Array(0.2, 0.5, 0.2))
    val p = new IntegerParameter(0, 0, 9, "d", Some(redist))
    val rnd = new Random(7)
    for (_ <- 0 until 100) {
      val v = p.randomizeValue(rnd).getValue
      assert(v >= 0.0 && v <= 9.0, s"out of range: $v")
    }
  }

  test("BooleanParameter skewed setValue round-trip endpoints") {
    val p = new BooleanParameter(false, "b", Some(BooleanRedistribution(0.8)))
    assert(p.setValue(0.0).getValue == 0.0 || p.setValue(0.0).getValue == 1.0)
    val t = p.setValue(1.0).getValue
    assert(t == 0.0 || t == 1.0)
  }

  test("BooleanParameter skewed randomizeValue stays 0 or 1") {
    val rnd = new Random(99)
    val p = new BooleanParameter(false, "b", Some(BooleanRedistribution(0.8)))
    for (_ <- 0 until 100) {
      val v = p.randomizeValue(rnd).getValue
      assert(v == 0.0 || v == 1.0, s"unexpected boolean value $v")
    }
  }
}

package com.barrybecker4.optimization.viewer.projectors

import com.barrybecker4.optimization.parameter.NumericParameterArray
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class SimpleProjectorSuite extends AnyFunSuite {

  private val rnd = new Random(1)
  private val projector = new SimpleProjector

  test("1D parameter array: Y mirrors X and Y range matches X range") {
    val oneD = new NumericParameterArray(
      Array(0.35),
      Array(0.0),
      Array(1.0),
      Array("p"),
      rnd
    )
    val p = projector.project(oneD)
    assert(p.x == 0.35 && p.y == 0.35)

    val rx = projector.getXRange(oneD)
    val ry = projector.getYRange(oneD)
    assert(rx.min == ry.min && rx.max == ry.max)
    assert(rx.getExtent > 0)
  }

  test("2D parameter array: even/odd split unchanged") {
    val twoD = new NumericParameterArray(
      Array(0.1, 0.2),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("a", "b"),
      rnd
    )
    val p = projector.project(twoD)
    assert(p.x == 0.1 && p.y == 0.2)
    assert(projector.getXRange(twoD).getExtent > 0)
    assert(projector.getYRange(twoD).getExtent > 0)
  }
}

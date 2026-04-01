package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.VariableLengthIntSet
import com.barrybecker4.optimization.parameter.VariableLengthIntSet.createParam
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class MagnitudeIgnoredDistanceCalculatorSuite extends AnyFunSuite {

  private val calc = new MagnitudeIgnoredDistanceCalculator()
  private val rnd = new Random(1)

  private def vl(ints: IndexedSeq[Int], full: IndexedSeq[Int]): VariableLengthIntSet = {
    val params = ints.map(createParam)
    new VariableLengthIntSet(params, full, calc, rnd)
  }

  test("identical multisets in different order have distance 0") {
    val a = vl(IndexedSeq(2, -1, 3), IndexedSeq(2, -1, 3, -4))
    val b = vl(IndexedSeq(3, 2, -1), IndexedSeq(2, -1, 3, -4))
    assertResult(0.0)(calc.calculateDistance(a, b))
  }

  test("length difference adds to distance") {
    val a = vl(IndexedSeq(2, -1), IndexedSeq(2, -1, 3))
    val b = vl(IndexedSeq(2, -1, 3), IndexedSeq(2, -1, 3))
    // |len diff| + multiset diff: 1 + 1 (extra 3 vs smaller multiset) = 2
    assertResult(2.0)(calc.calculateDistance(a, b))
  }

  test("same length different multiset has positive distance") {
    val a = vl(IndexedSeq(2, -1), IndexedSeq(2, -1, 3))
    val b = vl(IndexedSeq(2, 3), IndexedSeq(2, -1, 3))
    assert(calc.calculateDistance(a, b) > 0.0)
  }
}

package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.VariableLengthIntSet.createParam
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

  test("variable-length set crossover keeps fullSeq and valid subset") {
    val crossRnd = new Random(42)
    val fullSeq = IndexedSeq(2, -1, 3, -4)
    val v1 = VariableLengthIntSet.createInstance(IndexedSeq(createParam(2), createParam(-1)), fullSeq, crossRnd)
    val v2 = VariableLengthIntSet.createInstance(IndexedSeq(createParam(3), createParam(-4)), fullSeq, crossRnd)
    val c = ParameterCrossover.cross(v1, v2, crossRnd).asInstanceOf[VariableLengthIntSet]
    assert(c.fullSeq == fullSeq)
    assert(c.size >= 1 && c.size <= v1.getMaxLength)
    assert(c.intValues == c.intValues.distinct)
    assert(c.intValues.forall(fullSeq.contains))
  }

  test("variable-length crossover requires matching fullSeq") {
    val r = new Random(1)
    val v1 = VariableLengthIntSet.createInstance(IndexedSeq(createParam(1)), IndexedSeq(1, 2, 3), r)
    val v2 = VariableLengthIntSet.createInstance(IndexedSeq(createParam(2)), IndexedSeq(1, 2, 3, 4), r)
    intercept[IllegalArgumentException] {
      ParameterCrossover.cross(v1, v2, r)
    }
  }
}

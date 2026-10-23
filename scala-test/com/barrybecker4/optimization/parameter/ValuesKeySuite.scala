// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ValuesKeySuite extends AnyFunSuite {

  private def permutation(values: Int*): PermutedParameterArray =
    PermutedParameterArray(values.indices.map(i => IntegerParameter(values(i), 0, values.size - 1, "p" + i): Parameter), new Random(1))

  private def numeric(values: Double*): NumericParameterArray =
    NumericParameterArray(values.zipWithIndex.map { case (v, i) => DoubleParameter(v, -5000.0, 5000.0, "d" + i): Parameter }.toIndexedSeq, rnd = new Random(1))

  test("arrays with the same values have equal keys and hash codes") {
    val a = ValuesKey.of(permutation(2, 0, 1))
    val b = ValuesKey.of(permutation(2, 0, 1))
    assertResult(a)(b)
    assertResult(a.hashCode)(b.hashCode)
  }

  test("arrays with different values, or different sizes, have different keys") {
    assert(ValuesKey.of(permutation(2, 0, 1)) != ValuesKey.of(permutation(0, 2, 1)))
    assert(ValuesKey.of(permutation(0, 1)) != ValuesKey.of(permutation(0, 1, 2)))
  }

  test("positive and negative zero are the same value") {
    assertResult(ValuesKey.of(numeric(0.0, 1.5)))(ValuesKey.of(numeric(-0.0, 1.5)))
    assertResult(ValuesKey.of(numeric(0.0, 1.5)).hashCode)(ValuesKey.of(numeric(-0.0, 1.5)).hashCode)
  }

  test("values that differ by less than a printed number would show are still different keys") {
    assert(ValuesKey.of(numeric(1234.5671)) != ValuesKey.of(numeric(1234.5672)))
  }

  test("a key is not equal to something that is not a key") {
    assert(!ValuesKey.of(permutation(0, 1)).equals(Seq(0.0, 1.0)))
  }

  test("a key can be used in a set to find duplicates") {
    val keys = Seq(permutation(0, 1, 2), permutation(1, 0, 2), permutation(0, 1, 2)).map(ValuesKey.of)
    assertResult(2)(keys.toSet.size)
  }

  test("the default candidate identity is a values key") {
    assertResult(ValuesKey.of(permutation(1, 2, 0)))(permutation(1, 2, 0).candidateIdentity)
  }
}

// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.statespace

import com.barrybecker4.optimization.parameter.{NumericParameterArray, PermutedParameterArray}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** What counts as the same state when a search skips states it has already visited. */
class StateKeySuite extends AnyFunSuite {

  private class TaggedArray(tag: String, values: IndexedSeq[Parameter]) extends PermutedParameterArray(values, new Random(1)) {
    override def candidateIdentity: Any = (tag, super.candidateIdentity)
  }

  private def params(values: Int*): IndexedSeq[Parameter] =
    values.indices.map(i => IntegerParameter(values(i), 0, values.size - 1, "p" + i): Parameter)

  test("separately built arrays with the same values are the same state") {
    val a = PermutedParameterArray(params(1, 0, 2), new Random(1))
    val b = PermutedParameterArray(params(1, 0, 2), new Random(2))
    assertResult(StateSpaceSearchStrategy.stateKey(a))(StateSpaceSearchStrategy.stateKey(b))
  }

  test("arrays with different values are different states") {
    val a = PermutedParameterArray(params(1, 0, 2), new Random(1))
    val b = PermutedParameterArray(params(0, 1, 2), new Random(1))
    assert(StateSpaceSearchStrategy.stateKey(a) != StateSpaceSearchStrategy.stateKey(b))
  }

  test("an array whose identity is more than its values is told apart by it") {
    val a = new TaggedArray("one", params(1, 0, 2))
    val b = new TaggedArray("two", params(1, 0, 2))
    assert(StateSpaceSearchStrategy.stateKey(a) != StateSpaceSearchStrategy.stateKey(b))
  }

  test("numbers that differ only beyond what a printed number would show are different states") {
    val numeric = (v: Double) =>
      NumericParameterArray(IndexedSeq[Parameter](DoubleParameter(v, 0.0, 5000.0, "a")), rnd = new Random(1))
    assert(StateSpaceSearchStrategy.stateKey(numeric(1234.5671)) != StateSpaceSearchStrategy.stateKey(numeric(1234.5672)))
  }
}

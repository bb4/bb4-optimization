// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class CandidateSetSuite extends AnyFunSuite {

  private class TaggedArray(tag: String, values: IndexedSeq[Parameter]) extends PermutedParameterArray(values, new Random(1)) {
    override def candidateIdentity: Any = (tag, super.candidateIdentity)
  }

  private def params(values: Int*): IndexedSeq[Parameter] =
    values.indices.map(i => IntegerParameter(values(i), 0, values.size - 1, "p" + i): Parameter)

  private def permutation(values: Int*) = PermutedParameterArray(params(values*), new Random(values.sum))

  test("a new set is empty") {
    val set = new CandidateSet()
    assert(set.isEmpty && !set.contains(permutation(0, 1)))
  }

  test("a candidate is found by value, not by reference") {
    val set = new CandidateSet()
    set += permutation(2, 0, 1)
    assert(set.contains(permutation(2, 0, 1)))
    assert(!set.contains(permutation(0, 2, 1)))
  }

  test("adding a candidate that is already there leaves one") {
    val set = new CandidateSet()
    assert(set.add(permutation(2, 0, 1)))
    assert(!set.add(permutation(2, 0, 1)))
    assertResult(1)(set.size)
  }

  test("a candidate can be removed") {
    val set = new CandidateSet()
    set ++= Seq(permutation(2, 0, 1), permutation(0, 1, 2))
    set -= permutation(2, 0, 1)
    assertResult(1)(set.size)
    assert(!set.contains(permutation(2, 0, 1)) && set.contains(permutation(0, 1, 2)))
  }

  test("iterating gives the candidates that were added") {
    val set = new CandidateSet()
    val added = Seq(permutation(2, 0, 1), permutation(0, 1, 2), permutation(1, 2, 0))
    set ++= added
    assertResult(added.map(_.candidateIdentity).toSet)(set.iterator.map(_.candidateIdentity).toSet)
  }

  test("candidates with the same values but a different identity are different members") {
    val set = new CandidateSet()
    set += new TaggedArray("one", params(1, 0, 2))
    assert(!set.contains(new TaggedArray("two", params(1, 0, 2))))
    assert(set.contains(new TaggedArray("one", params(1, 0, 2))))
  }
}

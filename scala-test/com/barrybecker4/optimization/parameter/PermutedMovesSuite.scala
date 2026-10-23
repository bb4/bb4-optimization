// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** A move on a permutation is a list of swaps. It must give exactly the neighbor that getRandomNeighbor would. */
class PermutedMovesSuite extends AnyFunSuite {

  private def permutation(size: Int, seed: Long): PermutedParameterArray = {
    val order = new Random(seed).shuffle((0 until size).toList)
    PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, size - 1, "p" + i): Parameter), new Random(seed))
  }

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  test("a permutation supports moves, and a numeric array does not unless it says so") {
    assert(permutation(5, 1).supportsMoves)
    val numeric = NumericParameterArray(IndexedSeq[Parameter](
      com.barrybecker4.optimization.parameter.types.DoubleParameter(1.0, 0.0, 2.0, "a")), rnd = new Random(1))
    assert(!numeric.supportsMoves)
    intercept[UnsupportedOperationException](numeric.proposeMove(1.0))
    intercept[UnsupportedOperationException](numeric.applyMove(Swap(0, 0)))
  }

  test("applying a move gives the neighbor that getRandomNeighbor gives for the same random numbers") {
    for (size <- Seq(2, 3, 5, 12, 40, 200); radius <- Seq(0.1, 0.5, 1.0, 3.0, 20.0, 400.0, 1e6, 1e300); seed <- 1 to 4) {
      val viaNeighbor = permutation(size, seed).getRandomNeighbor(radius)
      val a = permutation(size, seed)
      val viaMove = a.applyMove(a.proposeMove(radius))
      assertResult(valuesOf(viaNeighbor), s"size=$size radius=$radius seed=$seed")(valuesOf(viaMove))
    }
  }

  test("a move uses the same random numbers as getRandomNeighbor, so the next draw is the same") {
    for (radius <- Seq(0.5, 5.0, 50.0, 1e6)) {
      val (a, b) = (permutation(30, 9), permutation(30, 9))
      a.getRandomNeighbor(radius)
      b.proposeMove(radius)
      assertResult(valuesOf(a.getRandomNeighbor(1.0)), s"radius $radius")(valuesOf(b.applyMove(b.proposeMove(1.0))))
    }
  }

  test("a small radius gives a single swap of two different positions") {
    val a = permutation(50, 3)
    for (_ <- 1 to 100) a.proposeMove(0.1) match {
      case Swap(i, j) => assert(i != j && i >= 0 && j >= 0 && i < 50 && j < 50)
      case other => fail("expected one swap but got " + other)
    }
  }

  test("a swap exchanges the values at two positions and leaves the original alone") {
    val a = permutation(6, 2)
    val before = valuesOf(a)
    val moved = a.applyMove(Swap(1, 4))
    assertResult(before.updated(1, before(4)).updated(4, before(1)))(valuesOf(moved))
    assertResult(before)(valuesOf(a))
  }

  test("a sequence of swaps is applied in order") {
    val a = permutation(6, 2)
    val before = valuesOf(a)
    val moved = a.applyMove(SwapSequence(Vector(Swap(0, 1), Swap(1, 2))))
    val expected = before.updated(0, before(1)).updated(1, before(0))
    assertResult(expected.updated(1, expected(2)).updated(2, expected(1)))(valuesOf(moved))
  }

  test("a move keeps each position's name and range") {
    val moved = permutation(8, 5).applyMove(Swap(0, 7))
    for (i <- 0 until 8) assertResult("p" + i)(moved.get(i).name)
  }

  test("a move of any size still gives a permutation") {
    val a = permutation(25, 4)
    for (radius <- Seq(0.2, 2.0, 30.0, 1e9)) assertResult(0 until 25)(valuesOf(a.applyMove(a.proposeMove(radius))).sorted)
  }

  test("an array of one or no parameters has an empty move") {
    val single = permutation(1, 1)
    assertResult(valuesOf(single))(valuesOf(single.applyMove(single.proposeMove(3.0))))
  }

  test("a move this array does not understand is an error") {
    intercept[UnsupportedOperationException](permutation(4, 1).applyMove(new Move {}))
  }
}

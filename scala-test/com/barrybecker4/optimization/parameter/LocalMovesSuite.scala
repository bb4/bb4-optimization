// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{BooleanParameter, DoubleParameter, IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** Moves that change one position: swaps for permutations, a new value for integers and booleans. */
class LocalMovesSuite extends AnyFunSuite {

  private def permutation(size: Int, seed: Long): PermutedParameterArray = {
    val order = new Random(seed).shuffle((0 until size).toList)
    PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, size - 1, "p" + i): Parameter), new Random(seed))
  }

  private def integers(values: Seq[Int], low: Int, high: Int): NumericParameterArray =
    NumericParameterArray(values.indices.map(i => IntegerParameter(values(i), low, high, "v" + i): Parameter), rnd = new Random(1))

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  test("a permutation has local moves, but a subclass that makes its own neighbors does not") {
    assert(permutation(6, 1).supportsLocalMoves)
    assert(!new PuzzleLikePath(permutation(6, 1).params, "path").supportsLocalMoves)
  }

  test("the moves at a position are a swap with each other position") {
    val moves = permutation(6, 1).movesAt(2).toList
    assertResult(List(Swap(2, 0), Swap(2, 1), Swap(2, 3), Swap(2, 4), Swap(2, 5)))(moves)
  }

  test("a random local move of a permutation swaps two different positions, and repeats for the same seed") {
    val array = permutation(8, 1)
    val random = new Random(7)
    val moves = (1 to 200).map(_ => array.proposeLocalMove(random))
    moves.foreach { case Swap(a, b) => assert(a != b && a >= 0 && b >= 0 && a < 8 && b < 8) }
    assertResult(array.proposeLocalMove(new Random(5)))(array.proposeLocalMove(new Random(5)))
    assert(moves.map { case Swap(a, _) => a }.toSet.size == 8, "every position gets chosen")
  }

  test("a swap creates the values in their new places and destroys them in their old ones") {
    val array = permutation(5, 2)
    val (a, b) = (array.get(1).getValue, array.get(3).getValue)
    assertResult(Seq(Placement(1, b), Placement(3, a)))(array.createdAttributes(Swap(1, 3)))
    assertResult(Seq(Placement(1, a), Placement(3, b)))(array.destroyedAttributes(Swap(1, 3)))
  }

  test("what a swap destroys is what the swap back would create, which is how a tabu list forbids undoing it") {
    val array = permutation(7, 3)
    val after = array.applyMove(Swap(2, 5))
    assertResult(array.destroyedAttributes(Swap(2, 5)))(after.createdAttributes(Swap(2, 5)))
  }

  test("a move that the array does not know is its own attribute") {
    val move = new Move {}
    assertResult(Seq(move))(permutation(4, 1).createdAttributes(move))
    assertResult(Seq(move))(integers(Seq(1, 2), 0, 3).destroyedAttributes(move))
  }

  test("an array of integers has local moves, and one with a real number does not") {
    assert(integers(Seq(0, 1, 2), 0, 3).supportsLocalMoves)
    val mixed = NumericParameterArray(IndexedSeq[Parameter](IntegerParameter(1, 0, 3, "a"), DoubleParameter(1.0, 0.0, 2.0, "b")))
    assert(!mixed.supportsLocalMoves)
    assert(!mixed.supportsMoves, "the neighbors of numeric arrays are still built the old way")
    intercept[UnsupportedOperationException](mixed.proposeMove(1.0))
  }

  test("an array of booleans has local moves") {
    val array = NumericParameterArray(IndexedSeq[Parameter](BooleanParameter(true, "a"), BooleanParameter(false, "b")))
    assert(array.supportsLocalMoves)
    assertResult(List(SetValue(0, 0.0)))(array.movesAt(0).toList)
    assertResult(List(SetValue(1, 1.0)))(array.movesAt(1).toList)
  }

  test("the moves at a position set it to each other value in its range") {
    val array = integers(Seq(2, 0, 1), 0, 3)
    assertResult(List(SetValue(0, 0.0), SetValue(0, 1.0), SetValue(0, 3.0)))(array.movesAt(0).toList)
  }

  test("a range that is large is sampled, so the number of moves is limited") {
    val array = integers(Seq(5), 0, 100_000)
    val moves = array.movesAt(0).toList
    assertResult(NumericParameterArray.MAX_VALUES_PER_POSITION)(moves.size)
    assert(moves.map { case SetValue(_, v) => v }.forall(v => v != 5.0 && v >= 0 && v <= 100_000))
  }

  test("a random local move of integers changes one position to a different value in range") {
    val array = integers(Seq(1, 2, 3, 0, 1), 0, 3)
    (1 to 200).foreach { seed =>
      val SetValue(index, value) = array.proposeLocalMove(new Random(seed)): @unchecked
      assert(index >= 0 && index < 5 && value >= 0 && value <= 3 && value != valuesOf(array)(index))
    }
  }

  test("a parameter with only one possible value is set to itself") {
    val SetValue(0, value) = integers(Seq(2), 2, 2).proposeLocalMove(new Random(1)): @unchecked
    assertResult(2.0)(value)
  }

  test("applying a move sets just that position, and leaves the array it was applied to as it was") {
    val array = integers(Seq(1, 2, 3), 0, 3)
    val after = array.applyMove(SetValue(1, 0.0))
    assertResult(Seq(1, 0, 3))(valuesOf(after))
    assertResult(Seq(1, 2, 3))(valuesOf(array))
  }

  test("a move that is not a value being set is refused") {
    intercept[UnsupportedOperationException](integers(Seq(1), 0, 3).applyMove(Swap(0, 0)))
  }

  test("setting a value creates it at the position and destroys the old one, so setting it back is tabu") {
    val array = integers(Seq(1, 2, 3), 0, 3)
    assertResult(Seq(Placement(1, 0.0)))(array.createdAttributes(SetValue(1, 0.0)))
    assertResult(Seq(Placement(1, 2.0)))(array.destroyedAttributes(SetValue(1, 0.0)))
    val after = array.applyMove(SetValue(1, 0.0))
    assertResult(array.destroyedAttributes(SetValue(1, 0.0)))(after.createdAttributes(SetValue(1, 2.0)))
  }

  test("an array that has no local moves refuses to give them") {
    val array = new PuzzleLikePath(permutation(4, 1).params, "path")
    intercept[UnsupportedOperationException](array.movesAt(0))
    intercept[UnsupportedOperationException](array.proposeLocalMove(new Random(1)))
  }
}

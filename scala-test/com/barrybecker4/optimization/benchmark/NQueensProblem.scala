// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, Swap}

import scala.util.Random

/**
  * Place n queens on an n by n board so that none attacks another. The tour is a permutation: the queen in row i
  * stands in column `p(i)`, so no two queens share a row or a column, and only the diagonals can conflict. The fitness
  * is the number of pairs of queens on the same diagonal, and 0 is a solution.
  *
  * This is the classic problem for min-conflicts and for tabu search with swaps. A swap changes the diagonals of just
  * two queens, so `evaluateMove` finds the new count by looking at those two queens alone.
  * @param size the number of queens; the known solution that is used as the exact one needs a size that is not 2 or 3
  *             more than a multiple of 6
  * @param seed decides the order in the initial guess
  */
class NQueensProblem(size: Int, seed: Long) extends OptimizeeProblem {

  require(size >= 4 && size % 6 != 2 && size % 6 != 3,
    s"The known solution is for sizes that are not 2 or 3 mod 6 (and at least 4), but got $size")

  override def getName: String = s"$size queens"

  override def evaluateByComparison: Boolean = false

  /** Count the queens on each diagonal; a diagonal with c queens has c * (c - 1) / 2 pairs. */
  override def evaluateFitness(board: ParameterArray): Double = {
    val rising = new Array[Int](2 * size)
    val falling = new Array[Int](2 * size)
    (0 until size).foreach { row =>
      val column = columnOf(board, row)
      rising(row + column) += 1
      falling(row - column + size) += 1
    }
    (pairs(rising) + pairs(falling)).toDouble
  }

  private def pairs(queensOnDiagonal: Array[Int]): Int = queensOnDiagonal.map(c => c * (c - 1) / 2).sum

  private def columnOf(board: ParameterArray, row: Int): Int = board.get(row).getValue.toInt

  /** Only the two swapped queens change which diagonals they are on, so only their conflicts are worked out again. */
  override def evaluateMove(board: ParameterArray, conflicts: Double, move: Move): Double = move match {
    case Swap(first, second) if first != second => conflicts + conflictChangeOfSwap(board, first, second)
    case Swap(_, _) => conflicts
    case _ => super.evaluateMove(board, conflicts, move)
  }

  private def conflictChangeOfSwap(board: ParameterArray, first: Int, second: Int): Int = {
    val (columnA, columnB) = (columnOf(board, first), columnOf(board, second))
    val before = attacks(board, first, columnA, first, second) + attacks(board, second, columnB, first, second) +
      attackIf(first, columnA, second, columnB)
    val after = attacks(board, first, columnB, first, second) + attacks(board, second, columnA, first, second) +
      attackIf(first, columnB, second, columnA)
    after - before
  }

  /** The number of queens, other than the two swapped ones, that a queen at (row, column) attacks. */
  private def attacks(board: ParameterArray, row: Int, column: Int, skipFirst: Int, skipSecond: Int): Int =
    (0 until size).count(other => other != skipFirst && other != skipSecond && onSameDiagonal(row, column, other, columnOf(board, other)))

  private def attackIf(rowA: Int, columnA: Int, rowB: Int, columnB: Int): Int =
    if (onSameDiagonal(rowA, columnA, rowB, columnB)) 1 else 0

  private def onSameDiagonal(rowA: Int, columnA: Int, rowB: Int, columnB: Int): Boolean =
    Math.abs(rowA - rowB) == Math.abs(columnA - columnB)

  override def getInitialGuess: ParameterArray = boardOf(new Random(seed + 1).shuffle((0 until size).toList), seed + 2)

  /** For sizes that are not 2 or 3 mod 6 the even columns followed by the odd ones is a solution. */
  override def getExactSolution: ParameterArrayWithFitness = {
    val board = boardOf((1 until size by 2).toList ++ (0 until size by 2).toList, seed)
    ParameterArrayWithFitness(board, evaluateFitness(board))
  }

  private def boardOf(columns: Seq[Int], randomSeed: Long): ParameterArray =
    PermutedParameterArray(columns.indices.map(row => new IntegerParameter(columns(row), 0, size - 1, "q" + row)), new Random(randomSeed))

  /** A random board has about as many attacking pairs as there are queens. */
  override def getFitnessRange: Double = size.toDouble
}

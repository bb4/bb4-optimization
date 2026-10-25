// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, PermutedParameterArray}

import scala.util.Random

/**
  * The all-interval series: arrange the numbers 0 to n - 1 so that the differences between neighbors are all
  * different, which makes them exactly 1 to n - 1. It comes from music (a twelve tone row that uses every interval).
  * The fitness is the number of those differences that no pair of neighbors has, and 0 is a solution.
  *
  * It is a good counterweight to N-queens for a benchmark: there are many plateaus, because changing one number moves
  * two differences at once and rarely helps, and there is no conflict to repair directly.
  * @param size how many numbers; at least 3
  * @param seed decides the order in the initial guess
  */
class AllIntervalProblem(size: Int, seed: Long) extends OptimizeeProblem {

  require(size >= 3, s"The series needs at least 3 numbers, but got $size")

  override def getName: String = s"All-interval series of $size"

  override def evaluateByComparison: Boolean = false

  /** The number of differences from 1 to n - 1 that are missing. */
  override def evaluateFitness(series: ParameterArray): Double = {
    val present = new Array[Boolean](size)
    (1 until size).foreach(i => present(difference(series, i)) = true)
    (1 until size).count(!present(_)).toDouble
  }

  /** The neighbors whose difference is also the difference of another pair of neighbors, both of them. */
  override def conflictedPositions(series: ParameterArray): Seq[Int] = {
    val timesSeen = new Array[Int](size)
    (1 until size).foreach(i => timesSeen(difference(series, i)) += 1)
    (1 until size).filter(i => timesSeen(difference(series, i)) > 1).flatMap(i => Seq(i - 1, i)).distinct
  }

  private def difference(series: ParameterArray, position: Int): Int = Math.abs(numberAt(series, position) - numberAt(series, position - 1))

  private def numberAt(series: ParameterArray, position: Int): Int = series.get(position).getValue.toInt

  override def getInitialGuess: ParameterArray = seriesOf(new Random(seed + 1).shuffle((0 until size).toList), seed + 2)

  /** Alternating the smallest and the largest number that is left gives the differences n - 1, n - 2, ..., 1. */
  override def getExactSolution: ParameterArrayWithFitness = {
    val series = seriesOf((0 until size).map(i => if (i % 2 == 0) i / 2 else size - 1 - i / 2), seed)
    ParameterArrayWithFitness(series, evaluateFitness(series))
  }

  private def seriesOf(numbers: Seq[Int], randomSeed: Long): ParameterArray =
    PermutedParameterArray(numbers.indices.map(i => new IntegerParameter(numbers(i), 0, size - 1, "n" + i)), new Random(randomSeed))

  /** A random series misses about a third of the differences. */
  override def getFitnessRange: Double = size.toDouble
}

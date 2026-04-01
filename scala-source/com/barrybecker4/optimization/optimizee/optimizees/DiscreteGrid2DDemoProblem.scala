// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.DiscreteStateSpace
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.IntegerParameter

import scala.util.Random

/**
  * Discrete 2D grid: same Euclidean-distance-to-target idea as [[DemoViewerProblem]], but parameters are
  * integer coordinates 0..`gridN` (normalized position `(x/gridN, y/gridN)`).
  * Implements [[DiscreteStateSpace]] with 4-neighbor moves for [[com.barrybecker4.optimization.strategy.OptimizationStrategyType.STATE_SPACE_SEARCH]].
  */
class DiscreteGrid2DDemoProblem extends OptimizeeProblem with DiscreteStateSpace {

  private val rnd = new Random(1)
  /** Coordinates are 0 .. gridN inclusive (matches [0,1] with step 1/gridN). */
  private val gridN = 20
  /** Target in normalized space (0.4, 0.4) → grid (8, 8). */
  private val targetGrid = (8, 8)
  private val targetNorm = (targetGrid._1.toDouble / gridN, targetGrid._2.toDouble / gridN)

  override def getName: String = "2D discrete grid (distance to target)"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue / gridN
    val y = params.get(1).getValue / gridN
    val dx = x - targetNorm._1
    val dy = y - targetNorm._2
    Math.sqrt(dx * dx + dy * dy)
  }

  override def getFitnessRange: Double = 1.5

  private def npa(x: Int, y: Int): NumericParameterArray = {
    val p1 = new IntegerParameter(x, 0, gridN, "p1")
    val p2 = new IntegerParameter(y, 0, gridN, "p2")
    new NumericParameterArray(IndexedSeq(p1, p2), rnd = rnd)
  }

  override def getExactSolution: ParameterArrayWithFitness =
    ParameterArrayWithFitness(npa(targetGrid._1, targetGrid._2), 0.0)

  override def getInitialGuess: ParameterArray = npa(gridN / 2, gridN / 2)

  override def successors(state: ParameterArray): Seq[ParameterArray] = {
    val x = state.get(0).getValue.toInt
    val y = state.get(1).getValue.toInt
    val moves = List((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))
    moves.filter { case (a, b) => a >= 0 && a <= gridN && b >= 0 && b <= gridN }.map { case (a, b) => npa(a, b) }
  }
}

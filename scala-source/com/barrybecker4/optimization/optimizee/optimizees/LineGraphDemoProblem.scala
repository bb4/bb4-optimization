// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.DiscreteStateSpace
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.IntegerParameter

import scala.util.Random

/**
  * One-dimensional integer line graph: fitness = |x − target|; successors are ±1 in range.
  * Mixes in [[DiscreteStateSpace]] so [[com.barrybecker4.optimization.strategy.OptimizationStrategyType.STATE_SPACE_SEARCH]]
  * works in [[com.barrybecker4.optimization.viewer.OptimizerEvalApp]].
  */
class LineGraphDemoProblem(
  val target: Int = 7,
  val lo: Int = 0,
  val hi: Int = 20
) extends OptimizeeProblem with DiscreteStateSpace {

  private val rnd = new Random(1)

  override def getName: String = "1D line (state-space demo)"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val v = params.get(0).getValue.toInt
    math.abs(v - target).toDouble
  }

  override def getFitnessRange: Double = math.max(hi - target, target - lo).toDouble

  private def npa(value: Int): NumericParameterArray = {
    val p = new IntegerParameter(value, lo, hi, "x")
    new NumericParameterArray(IndexedSeq(p), rnd = rnd)
  }

  override def getExactSolution: ParameterArrayWithFitness =
    ParameterArrayWithFitness(npa(target), 0.0)

  override def getInitialGuess: ParameterArray = npa((lo + hi) / 2)

  override def successors(state: ParameterArray): Seq[ParameterArray] = {
    val v = state.get(0).getValue.toInt
    List(v - 1, v + 1).filter(x => x >= lo && x <= hi).map(npa)
  }
}

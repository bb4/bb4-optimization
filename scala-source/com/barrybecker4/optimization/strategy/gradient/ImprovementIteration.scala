// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.linear.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.{Direction, NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

/**
  * Utility class for maintaining the data vectors for the iteration when hill climbing
  * over a numerical parameter space.
  * @param params the params to improve.
  * @param numericPa same as `params.pa` as numeric array (caller guarantees type).
  * @param priorGradient direction from the previous step, if any; otherwise a default unit direction is used for dot-product step sizing.
  * @author Barry Becker
  */
class ImprovementIteration(
  params: ParameterArrayWithFitness,
  numericPa: NumericParameterArray,
  priorGradient: Option[Vector] = None
) {

  private var delta: Vector = numericPa.asVector
  private var fitnessDelta: Vector = numericPa.asVector
  var gradient: Vector = numericPa.asVector

  /** Prior step's gradient for comparing turning angle; `None` uses a normalized all-ones direction. */
  val previousGradientForDot: Vector = priorGradient match {
    case Some(g) => g
    case None =>
      var og = numericPa.asVector
      for (i <- 0 until numericPa.size) {
        og = og.set(i, 1.0)
      }
      og.normalize
  }

  /** Compute the squares in one of the iteration directions and add it to the running sum.
    * @return the sum of squares in one of the iteration directions.
    */
  def incSumOfSqs(i: Int, optimizee: Optimizee): Double = {

    val p: Parameter = numericPa.get(i)
    val forwardParams = numericPa.incrementByEps(i, Direction.FORWARD)
    val fwdFitnessDelta = findFitnessDelta(optimizee, params, forwardParams)
    val backwardParams = numericPa.incrementByEps(i, Direction.BACKWARD)
    val bwdFitnessDelta = findFitnessDelta(optimizee, params, backwardParams)

    val baseValue = p.getValue
    val forwardValue = forwardParams.get(i).getValue
    val backwardValue = backwardParams.get(i).getValue
    val hasForwardStep = forwardValue != baseValue
    val hasBackwardStep = backwardValue != baseValue

    val (effectiveFitnessDelta, effectiveDelta) =
      if (hasForwardStep && hasBackwardStep) {
        // Central difference: (f(x+h) - f(x-h)) / (2h)
        (fwdFitnessDelta - bwdFitnessDelta, forwardValue - backwardValue)
      } else if (hasForwardStep) {
        (fwdFitnessDelta, forwardValue - baseValue)
      } else if (hasBackwardStep) {
        (bwdFitnessDelta, backwardValue - baseValue)
      } else {
        (0.0, p.getIncrementForDirection(Direction.FORWARD))
      }

    fitnessDelta = fitnessDelta.set(i, effectiveFitnessDelta)
    delta = delta.set(i, effectiveDelta)
    val d = delta(i)
    if (d == 0.0) 0.0 else (effectiveFitnessDelta * effectiveFitnessDelta) / (d * d)
  }

  /** @param optimizee the thing being optimized
    * @param params the current parameters
    * @param testParams the new set of parameters being evaluated.
    * @return the incremental change in fitness. More negative means bigger move toward goal.
    *         A positive value means it is moving away from goal.
    */
  private def findFitnessDelta(optimizee: Optimizee,
                               params: ParameterArrayWithFitness,
                               testParams: ParameterArray): Double = {
    if (optimizee.evaluateByComparison) optimizee.compareFitness(testParams, params.pa)
    else optimizee.evaluateFitness(testParams) - params.fitness
  }

  /** Update gradient. Use EPS if the gradLength is 0. */
  def updateGradient(jumpSize: Double, gradLength: Double): Unit = {
    val gradLen: Double = if (gradLength == 0) MathUtil.EPS_MEDIUM else gradLength
    for (i <- 0 until delta.size) {
      val safeDelta = if (Math.abs(delta(i)) < MathUtil.EPS_MEDIUM) MathUtil.EPS_MEDIUM else delta(i)
      val denominator = safeDelta * gradLen
      gradient = gradient.set(i, -jumpSize * fitnessDelta(i) / denominator)
    }
  }
}

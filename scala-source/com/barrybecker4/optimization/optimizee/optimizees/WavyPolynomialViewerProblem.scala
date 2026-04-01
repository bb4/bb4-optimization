// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

import scala.util.Random

/**
  * Two-parameter continuous optimizee for [[com.barrybecker4.optimization.viewer.OptimizerEvalApp]] with a
  * highly non-convex landscape: a quadratic bowl plus squared sinusoids and a coupled term, all constructed
  * so the unique global minimum remains exactly at `(cx, cy)` in `[0,1]²` with fitness `0`.
  *
  * `f(x,y) = (x-cx)² + (y-cy)² + a·sin²(kπx)sin²(kπy) + b·((x-cx)(y-cy))²·sin²(mπx)sin²(mπy)`
  */
class WavyPolynomialViewerProblem(
    val cx: Double = 0.4,
    val cy: Double = 0.6,
    val rippleAmplitude: Double = 0.15,
    val rippleFreq: Int = 10,
    val coupledAmplitude: Double = 0.05,
    val coupledFreq: Int = 8
) extends OptimizeeProblem {

  private val rnd = new Random(1)

  override def getName: String = "2D wavy polynomial (many local minima)"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val x = params.get(0).getValue
    val y = params.get(1).getValue
    val dx = x - cx
    val dy = y - cy
    val k = rippleFreq * math.Pi
    val m = coupledFreq * math.Pi
    val sinKX = math.sin(k * x)
    val sinKY = math.sin(k * y)
    val sinMX = math.sin(m * x)
    val sinMY = math.sin(m * y)
    dx * dx + dy * dy +
      rippleAmplitude * sinKX * sinKX * sinKY * sinKY +
      coupledAmplitude * dx * dx * dy * dy * sinMX * sinMX * sinMY * sinMY
  }

  /** Upper bound on fitness over the unit square (conservative for viewer scaling). */
  override def getFitnessRange: Double = 2.5

  override def getExactSolution: ParameterArrayWithFitness = ParameterArrayWithFitness(
    new NumericParameterArray(
      Array(cx, cy),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("x", "y"),
      rnd
    ),
    0.0
  )

  /** Start away from the basin to make the search visibly harder. */
  override def getInitialGuess: ParameterArray =
    new NumericParameterArray(
      Array(0.15, 0.85),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("x", "y"),
      rnd
    )
}

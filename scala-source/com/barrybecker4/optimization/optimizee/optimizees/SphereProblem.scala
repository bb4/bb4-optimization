package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.DoubleParameter

/**
 * The Sphere function (De Jong's function 1).
 * Continuous, strictly convex, and unimodal. It serves as the baseline
 * test for continuous convergence speed and precision.
 *
 * f(x) = \sum_{i=1}^{d} x_i^2
 * Global minimum: f(0, ..., 0) = 0
 */
case class SphereProblem(dimensions: Int = 2) extends OptimizeeProblem {

  private val domainMin: Double = -5.12
  private val domainMax: Double = 5.12

  override def getName: String = s"Sphere Problem ($dimensions-D)"

  override def evaluateFitness(params: ParameterArray): Double = {
    var sum = 0.0
    for (i <- 0 until params.size) {
      val x = params.get(i).getValue
      sum += x * x
    }
    sum
  }

  override def evaluateByComparison: Boolean = false
  
  override def getInitialGuess: ParameterArray = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      // Start away from the origin (e.g. 3.0) to test convergence
      val startVal = 3.0
      params(i) = new DoubleParameter(startVal, domainMin, domainMax, s"x_$i")
    }
    NumericParameterArray(params.toIndexedSeq)
  }

  override def getExactSolution: ParameterArrayWithFitness = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      params(i) = new DoubleParameter(0.0, domainMin, domainMax, s"x_$i")
    }
    ParameterArrayWithFitness(new NumericParameterArray(params.toIndexedSeq), 0)
  }

  override def getFitnessRange: Double = 100.0
}
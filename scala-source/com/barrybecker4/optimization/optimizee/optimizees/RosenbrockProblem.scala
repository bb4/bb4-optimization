package com.barrybecker4.optimization.optimizee.optimizees

//import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.DoubleParameter

/**
 * The Rosenbrock "Banana Valley" function. The global minimum is inside a long,
 * narrow, parabolic-shaped flat valley. Finding the valley is trivial, but
 * converging to the global minimum is difficult.
 *
 * Global minimum: f(1, ..., 1) = 0
 */
case class RosenbrockProblem(dimensions: Int = 2) extends OptimizeeProblem {

  private val domainMin: Double = -5.0
  private val domainMax: Double = 10.0

  override def getName: String = s"Rosenbrock Problem ($dimensions-D)"

  override def evaluateFitness(params: ParameterArray): Double = {
    var sum = 0.0
    for (i <- 0 until params.size - 1) {
      val xi = params.get(i).getValue
      val xiPlus1 = params.get(i + 1).getValue
      val term1 = xiPlus1 - (xi * xi)
      val term2 = 1.0 - xi
      sum += (100.0 * term1 * term1) + (term2 * term2)
    }
    sum
  }

  override def evaluateByComparison: Boolean = false

  override def getInitialGuess: ParameterArray = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      // Standard starting point for Rosenbrock benchmark is (-1.2, 1.0) or negative space
      val startVal = if (i % 2 == 0) -1.2 else 1.0
      params(i) = new DoubleParameter(startVal, domainMin, domainMax, s"x_$i")
    }
    NumericParameterArray(params.toIndexedSeq)
  }

  override def getExactSolution: ParameterArrayWithFitness = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      params(i) = new DoubleParameter(1.0, domainMin, domainMax, s"x_$i")
    }
    ParameterArrayWithFitness(NumericParameterArray(params.toIndexedSeq), 0)
  }

  override def getFitnessRange: Double = 2000.0
}

package com.barrybecker4.optimization.optimizee.optimizees

//import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.DoubleParameter

import scala.math.*

/**
 * The Rastrigin function is a non-convex function used as a performance test problem
 * for optimization algorithms. It features a large number of regular local minima.
 *
 * Global minimum: f(0, ..., 0) = 0
 */
case class RastriginProblem(dimensions: Int = 2) extends OptimizeeProblem {

  private val domainMin: Double = -5.12
  private val domainMax: Double = 5.12
  private val aConst: Double = 10.0

  override def getName: String = s"Rastrigin Problem ($dimensions-D)"

  override def evaluateFitness(params: ParameterArray): Double = {
    var fitness = aConst * params.size
    for (i <- 0 until params.size) {
      val x = params.get(i).getValue
      fitness += (x * x) - (aConst * cos(2.0 * Pi * x))
    }
    fitness
  }

  override def evaluateByComparison: Boolean = false

  override def getInitialGuess: ParameterArray = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      // Start in a deceptive outer local basin
      val startVal = 4.5
      params(i) = new DoubleParameter(startVal, domainMin, domainMax, s"x_$i")
    }
    new NumericParameterArray(params.toIndexedSeq)
  }

  override def getExactSolution: ParameterArrayWithFitness = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      params(i) = new DoubleParameter(0.0, domainMin, domainMax, s"x_$i")
    }
    ParameterArrayWithFitness(new NumericParameterArray(params.toIndexedSeq), 0)
  }

  override def getFitnessRange: Double = 80.0
}

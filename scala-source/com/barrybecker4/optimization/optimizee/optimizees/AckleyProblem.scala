package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.parameter.types.DoubleParameter

import scala.math.*

/**
 * The Ackley function is widely used for testing optimization algorithms.
 * In its 2D (or N-D) form, it is characterized by a nearly flat outer region
 * and a central hole surrounded by a cosine ripple lattice.
 *
 * Global minimum: f(0, ..., 0) = 0
 */
case class AckleyProblem(dimensions: Int = 2) extends OptimizeeProblem {

  private val domainMin: Double = -32.768
  private val domainMax: Double = 32.768
  private val initialRange: Double = domainMax - domainMin

  override def getName: String = s"Ackley Problem ($dimensions-D)"

  override def evaluateFitness(params: ParameterArray): Double = {
    val n = params.size.toDouble
    var sumSq = 0.0
    var sumCos = 0.0

    for (i <- 0 until params.size) {
      val x = params.get(i).getValue
      sumSq += x * x
      sumCos += cos(2.0 * Pi * x)
    }

    val term1 = -20.0 * exp(-0.2 * sqrt(sumSq / n))
    val term2 = -exp(sumCos / n)
    term1 + term2 + 20.0 + E
  }

  override def evaluateByComparison: Boolean = false

  override def getInitialGuess: ParameterArray = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      // Start away from the origin to test exploration down the ripple basin
      val startVal = domainMin + 0.75 * initialRange
      params(i) = new DoubleParameter(startVal, domainMin, domainMax, s"x_$i")
    }
    new NumericParameterArray(params.toIndexedSeq)
  }

  override def getExactSolution: ParameterArrayWithFitness = {
    val params = new Array[DoubleParameter](dimensions)
    for (i <- 0 until dimensions) {
      params(i) = new DoubleParameter(0.0, domainMin, domainMax, s"x_$i")
    }
    ParameterArrayWithFitness(NumericParameterArray(params.toIndexedSeq), 0)
  }

  override def getFitnessRange: Double = 25.0
}

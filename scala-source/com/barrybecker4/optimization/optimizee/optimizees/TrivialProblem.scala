// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import TrivialProblem._

import scala.util.Random


object TrivialProblem {
  private val SOLUTION_VALUE = 0.4

  val EXACT_SOLUTION = ParameterArrayWithFitness(
    new NumericParameterArray(
      Array(SOLUTION_VALUE), Array(0.0), Array(1.0), Array[String]("param1"), new Random(1)), 0.0)
}

/**
  * A trivial one dimensional example implementation of an OptimizeeProblem
  * @author Barry Becker
  */
class TrivialProblem extends OptimizeeProblem {

  override def getExactSolution: ParameterArrayWithFitness = EXACT_SOLUTION

  override def getInitialGuess = new NumericParameterArray(
    Array(0.5), Array(0.0), Array(1.0), Array[String]("param1"), new Random(1)
  )

  override def getFitnessRange = 1.0
  override def getName = "Trivial Test Problem"
  override def evaluateByComparison = false
  override def evaluateFitness(params: ParameterArray): Double = EXACT_SOLUTION.pa.distance(params)
}
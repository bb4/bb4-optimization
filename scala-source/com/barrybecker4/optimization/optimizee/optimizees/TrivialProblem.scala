// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter1.NumericParameterArray
import com.barrybecker4.optimization.parameter1.ParameterArray
import TrivialProblem._


object TrivialProblem {
  private val SOLUTION_VALUE = 0.4

  val EXACT_SOLUTION = new NumericParameterArray(
    Array[Double](SOLUTION_VALUE), Array[Double](0.0), Array[Double](1.0), Array[String]("param1")
  )
}

/**
  * A trivial one dimensional example implementation of an OptimizeeProblem
  * @author Barry Becker
  */
class TrivialProblem extends OptimizeeProblem {

  override def getExactSolution: ParameterArray = EXACT_SOLUTION

  override def getInitialGuess = new NumericParameterArray(
    Array[Double](0.5), Array[Double](0.0), Array[Double](1.0), Array[String]("param1")
  )

  override def getFitnessRange = 1.0
  override def getName = "Trivial Test Problem"
  override def evaluateByComparison = false
  override def evaluateFitness(params: ParameterArray): Double = EXACT_SOLUTION.distance(params)
}
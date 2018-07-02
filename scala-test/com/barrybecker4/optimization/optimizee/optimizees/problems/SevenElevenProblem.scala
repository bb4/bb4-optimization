// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite.LOG_FILE_HOME
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.strategy.{GLOBAL_SAMPLING, OptimizationStrategyType}

import scala.util.Random

/**
  * This is a simple search space to test the optimization package.
  * The problem we will try to solve is :
  *
  * p1 + p2 + p3 + p4  = 711
  * p1 * p2 * p3 * p4  = 711000000
  *
  * Which corresponds to the problem of someone going into a 7-11 and buying 4 things
  * whose sum and product equal $7.11.
  * This problem can be solved analytically by finding the prime factors of 711 and
  * eliminating combinations until you are left with:
  * 316, 125, 120, 150
  * as being the only solution.
  * Our choice of evaluation function to maximize is somewhat arbitrary.
  * When this function evaluates to 0, we have a solution.
  *
  * @see ParabolaMinFunctionProblem for an easier optimization example.
  * @author Barry Becker
  */
object SevenElevenProblem {
  private val INITIAL_GUESS_PARAMS: Array[Parameter] = Array(
    new IntegerParameter(100, 0, 708, "p1"),
    new IntegerParameter(200, 0, 708, "p2"),
    new IntegerParameter(200, 0, 708, "p3"),
    new IntegerParameter(200, 0, 708, "p4")
  )
  private val P1 = 316
  private val P2 = 125
  private val P3 = 120
  private val P4 = 150
  private val RAND = new Random(1)
  /** these may be in any order, however */
  private val EXACT_SOLUTION_PARAMS: Array[Parameter] = Array(
    new IntegerParameter(P1, 0, 708, "p1"),
    new IntegerParameter(P2, 0, 708, "p2"),
    new IntegerParameter(P3, 0, 708, "p3"),
    new IntegerParameter(P4, 0, 708, "p4")
  )
  private val INITIAL_GUESS = new NumericParameterArray(INITIAL_GUESS_PARAMS, RAND)
  private val EXACT_SOLUTION = new NumericParameterArray(EXACT_SOLUTION_PARAMS, RAND)
  // @@ exp errors.
  private val FITNESS_RANGE = 5000000.0

  /** This finds the solution for the above optimization problem.  */
  def main(args: Array[String]): Unit = {
    val problem = new SevenElevenProblem
    val optimizer = new Optimizer(problem, Some(LOG_FILE_HOME + "seven11_optimization.txt"))
    val initialGuess = problem.getInitialGuess
    val solution = optimizer.doOptimization(GLOBAL_SAMPLING, initialGuess, FITNESS_RANGE, RAND)
    OptimizeeProblem.showSolution(problem, solution)
  }
}

class SevenElevenProblem extends OptimizeeProblem {

  /** Evaluate directly, not by comparing with a different trial. */
  override def evaluateByComparison = false

  override def getName = "Seven Eleven Problem"

  /** The choice of fitness function here is somewhat arbitrary.
    * I chose to use:
    * -bs( p1 + p2 + p3 + p4 - 711) cubed + abs(711000000 - p1 * p2 * p3 * p4)
    * or
    * abs(711 - sum) + abs(711000000 - product)/1000000
    * This is 0 when the constraints are satisfied, something greater than 0 when not.
    * @param a the position in the search space given values of p1, p2, p4, p4.
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double = {
    val sum = a.get(0).getValue + a.get(1).getValue + a.get(2).getValue + a.get(3).getValue
    val product = a.get(0).getValue * a.get(1).getValue * a.get(2).getValue * a.get(3).getValue
    Math.abs(711.0 - sum) + Math.abs(711000000.0 - product) / 1000000.0
  }

  override def getExactSolution: ParameterArray = SevenElevenProblem.EXACT_SOLUTION

  override def getInitialGuess: ParameterArray = SevenElevenProblem.INITIAL_GUESS

  override def getFitnessRange: Double = SevenElevenProblem.FITNESS_RANGE
}

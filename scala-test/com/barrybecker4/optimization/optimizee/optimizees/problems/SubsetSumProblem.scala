// Copyright by Barry G. Becker, 2013-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite.LOG_FILE_HOME
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness, VariableLengthIntSet}
import com.barrybecker4.optimization.strategy.{GLOBAL_HILL_CLIMBING, OptimizationStrategyType}

import scala.util.Random


object SubsetSumProblem {

  /** This finds the solution for the above optimization problem. */
  def main(args: Array[String]): Unit = {
    val problem = new SubsetSumProblem(NO_SOLUTION)
    val optimizer = new Optimizer(problem, Some(LOG_FILE_HOME + "domSet_optimization.txt"))
    val initialGuess = problem.getInitialGuess
    //System.out.println("initial guess=" + initialGuess + " all=" + initialGuess.asInstanceOf[VariableLengthIntSet].getMaxLength)
    val solution = optimizer.doOptimization(GLOBAL_HILL_CLIMBING,
      initialGuess, problem.getFitnessRange, new Random(1))
    OptimizeeProblem.showSolution(problem, solution)
  }
}

/**
  * Determining if a set of numbers has a subset that sums to 0 is NP-Complete.
  * The only strategy that is guaranteed to find a solution if it exists is brute force search.
  * http://en.wikipedia.org/wiki/Subset_sum_problem
  * @author Barry Becker
  */
class SubsetSumProblem(var variation: SubsetSumVariation) extends OptimizeeProblem[NumericParameterArray] {

  override def getName: String = "Subset Sum: " + variation.getName

  /** Evaluate directly not by comparing with a different trial. */
  override def evaluateByComparison = false

  /** Use the cost matrix for the TSP variation to determine this.
    * @return fitness value
    */
  override def evaluateFitness(a: NumericParameterArray): Double = variation.evaluateFitness(a)

  override def getExactSolution: ParameterArrayWithFitness = variation.getExactSolution

  override def getInitialGuess: NumericParameterArray = variation.getInitialGuess

  override def getFitnessRange: Double = variation.getFitnessRange
}

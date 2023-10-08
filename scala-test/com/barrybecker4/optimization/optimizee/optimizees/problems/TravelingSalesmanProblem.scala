// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite.LOG_FILE_HOME
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.SIMULATED_ANNEALING
import OptimizeeProblem.showSolution

import scala.util.Random


object TravelingSalesmanProblem {

  /** Finds the solution for the above optimization problem. */
  def main(args: Array[String]): Unit = {
    val v = TSP_SIMPLE // TSP_STANDARD // TSP_US_CAPITALS // TSP_SIMPLE
    val problem = new TravelingSalesmanProblem(v)
    val optimizer = new Optimizer(problem, Some(LOG_FILE_HOME + "tsp_optimization.txt"))
    val initialGuess = problem.getInitialGuess

    val solution = optimizer.doOptimization(SIMULATED_ANNEALING,
      initialGuess, v.getFitnessRange, new Random(1))

    showSolution(problem, solution)
  }
}

/**
  * This is a simple search space to test the optimization package.
  * @author Barry Becker
  */
class TravelingSalesmanProblem(val variation: TravelingSalesmanVariation = TSP_SIMPLE)
    extends OptimizeeProblem {

  override def getName: String = "TSP: " + variation.getName

  /** Evaluate directly not by comparing with a different trial */
  override def evaluateByComparison = false

  /** Use the cost matrix for the TSP variation to determine this.
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double = variation.evaluateFitness(a)

  override def getExactSolution: ParameterArrayWithFitness = variation.getExactSolution

  override def getInitialGuess: ParameterArray = variation.getInitialGuess

  override def getFitnessRange: Double = variation.getFitnessRange
}

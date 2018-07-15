// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.{OptimizationStrategyType, SIMULATED_ANNEALING}
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite.LOG_FILE_HOME
import OptimizeeProblem.showSolution

import scala.util.Random


/**
  * See these references to help understand the problem of finding dominating sets given a graph.
  * http://csunplugged.org/dominating-sets (this is where I got the inspiration to add this class)
  * http://en.wikipedia.org/wiki/Dominating_set
  * This is a simple search example to help test the optimization package.
  * @author Barry Becker
  */
object DominatingSetProblem {

  /** This finds the solution for the above optimization problem. */
  def main(args: Array[String]): Unit = {
    val v = TYPICAL_DS
    val problem = new DominatingSetProblem(v)
    val optimizer = new Optimizer(problem, Some(LOG_FILE_HOME + "domSet_optimization.txt"))
    val initialGuess = problem.getInitialGuess
    val solution = optimizer.doOptimization(SIMULATED_ANNEALING,
      initialGuess, v.getFitnessRange, new Random(1))
    showSolution(problem, solution)
  }
}

class DominatingSetProblem(var variation: DominatingSetVariation) extends OptimizeeProblem {

  override def getName: String = "Dominating Set: " + variation.getName

  /** Evaluate directly not by comparing with a different trial. */
  override def evaluateByComparison = false

  /** Use the cost matrix for the TSP variation to determine this.
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double = variation.evaluateFitness(a)

  override def getExactSolution: ParameterArrayWithFitness = variation.getExactSolution
  override def getInitialGuess: ParameterArray = variation.getInitialGuess
  override def getFitnessRange: Double = variation.getFitnessRange
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee1.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray


object OptimizeeProblem {
  def showSolution(problem: OptimizeeProblem, solution: ParameterArray): Unit = {
    System.out.println("\n************************************************************************")
    System.out.println("The solution to the " + problem.getName + " test problem is :\n" + solution)
    System.out.println("Which evaluates to: " + problem.evaluateFitness(solution))
    System.out.println("We expected to get exactly " + problem.getExactSolution)
  }
}

/**
  * Abstract base class for optimizer test problems.
  * @author Barry Becker
  */
abstract class OptimizeeProblem extends Optimizee {

  /** @return the exact solution for this problem. */
  def getExactSolution: ParameterArray

  def getInitialGuess: ParameterArray

  /** @return distance from the exact solution as the error. */
  def getError(solution: ParameterArray): Double =
    100.0 * (solution.getFitness - getOptimalFitness) / getFitnessRange

  override def getOptimalFitness = 0
  override def compareFitness(a: ParameterArray, b: ParameterArray) = 0.0
  override def toString: String = getName

  /** @return approximate range of fitness values (usually 0 to this number). */
  def getFitnessRange: Double
}
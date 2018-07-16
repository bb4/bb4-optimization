// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}


object OptimizeeProblem {

  def showSolution(problem: OptimizeeProblem, solution: ParameterArrayWithFitness): Unit = {
    println("\n************************************************************************")
    println("The solution to the " + problem.getName + " test problem is :\n" + solution)
    println("Which evaluates to: " + solution.fitness)
    println("We expected to get exactly " + problem.getExactSolution)
  }
}

/**
  * Abstract base class for optimizer test problems.
  * @author Barry Becker
  */
abstract class OptimizeeProblem extends Optimizee {

  /** @return the exact solution for this problem. */
  def getExactSolution: ParameterArrayWithFitness

  def getInitialGuess: ParameterArray

  /** @return distance from the exact solution as the error. */
  def getError(solution: ParameterArrayWithFitness): Double =
    100.0 * (solution.fitness - getOptimalFitness) / getFitnessRange

  override def getOptimalFitness = 0
  override def compareFitness(a: ParameterArray, b: ParameterArray) =
    throw new UnsupportedOperationException("compareFitness not supperted for " + this.getClass.getName)
  override def toString: String = getName

  /** @return approximate range of fitness values (usually 0 to this number). */
  def getFitnessRange: Double
}
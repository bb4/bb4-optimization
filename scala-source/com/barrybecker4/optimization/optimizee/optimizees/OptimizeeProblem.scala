// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}


object OptimizeeProblem {

  def showSolution(problem: OptimizeeProblem[ParameterArray], solution: ParameterArrayWithFitness[ParameterArray]): Unit = {
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
abstract class OptimizeeProblem[P <: ParameterArray] extends Optimizee[P] {

  /** @return the exact solution for this problem. */
  def getExactSolution: ParameterArrayWithFitness[P]

  def getInitialGuess: P

  /** @return distance from the exact solution as the error. */
  def getError(solution: ParameterArrayWithFitness[P]): Double =
    100.0 * (solution.fitness - getOptimalFitness) / getFitnessRange

  override def getOptimalFitness = 0
  override def compareFitness(a: P, b: P) =
    throw new UnsupportedOperationException("compareFitness not supported for " + this.getClass.getName)
  override def toString: String = getName

  /** @return approximate range of fitness values (usually 0 to this number). */
  def getFitnessRange: Double
}
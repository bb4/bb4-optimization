package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.optimizee.optimizees.problems.{ParabolaFunctionConsts, ParabolaMinVariation}
import com.barrybecker4.optimization.parameter.ParameterArray

/**
  * This is a simple search space to test the optimization package.
  * The function we will try to minimize is
  *
  * z = (1 - p1)^2 + (2 - p2)^2
  *
  * Normally we have no idea what the real function is that we are trying to optimize.
  * Nor is a real life function likely to be as well behaved as this one is.
  * This function is very smooth (actually infinitely differentiable) - which is a
  * feature that makes hill-climbing algorithms work very well on.
  * For this simple case, I intentionally use a simple polynomial function with only
  * 2 parameters so that I can solve it analytically and compare it to the optimization results.
  * For this function the global maximum is 1 and it occurs only when p1 = 1 and p2 = 2.
  * There are no other local maxima. The shape of the surface formed by this function
  * is an inverted parabola centered at p1 = 1 and p2 = 2.
  *
  * There are a few variations on the analytic function to choose from, but they all have the same solution.
  *
  * @see SevenElevenProblem for somewhat harder example.
  * @author Barry Becker
  */
object ParabolaMinFunctionProblem {
  private val FITNESS_RANGE = 1000.0
}

class ParabolaMinFunctionProblem(var variation: ParabolaMinVariation) extends OptimizeeProblem {
  override def getName: String = "Analytic: " + variation.getClass.getName

  /** Evaluate directly not by comparing with a different trial */
  override def evaluateByComparison = false

  /** @param a the position on the parabolic surface given the specified values of p1 and p2
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double = variation.evaluateFitness(a)

  override def getInitialGuess: ParameterArray = ParabolaFunctionConsts.INITIAL_GUESS

  override def getExactSolution: ParameterArray = ParabolaFunctionConsts.EXACT_SOLUTION

  override def getFitnessRange: Double = ParabolaMinFunctionProblem.FITNESS_RANGE
}

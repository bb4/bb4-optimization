// Copyright by Barry G. Becker, 2013-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees.problems

import com.barrybecker4.optimization.optimizee.optimizees.ProblemVariation
import com.barrybecker4.optimization.optimizee.optimizees.ErrorTolerances
import ErrorTolerances._
import com.barrybecker4.optimization.optimizee.optimizees.problems.ParabolaFunctionConsts._
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

object ParabolaMinVariation {
  val VALUES = IndexedSeq(PARABOLA, SINUSOIDAL, ABS_SINUSOIDAL, STEPPED)
}
/**
  * An enum for different sorts of analytic functions that we might want to test.
  * Different types of 3d planar functions that all have the same maximum.
  * @author Barry Becker
  */
sealed trait ParabolaMinVariation extends ProblemVariation {

  /** Evaluate fitness for the analytics function. Lower values are more fit.
    * @param a the position on the parabolic surface given the specified values of p1 and p2
    * @return fitness value
    */
  def evaluateFitness(a: ParameterArray): Double

  override def getExactSolution: ParameterArray = ParabolaFunctionConsts.EXACT_SOLUTION

  /** @return the error tolerance percent for a specific optimization strategy */
  override def getErrorTolerancePercent(opt: OptimizationStrategyType): Double =
    errorTolerances.getErrorTolerancePercent(opt)

  /** Error tolerance for each search strategy and variation of the problem.
    * @return error tolerance percent
    */
  protected def errorTolerances: ErrorTolerances
}


case object PARABOLA extends ParabolaMinVariation {
  val errorTolerances = ErrorTolerances(
    0.009, RELAXED_TOL, BASE_TOLERANCE, GLOB_SAMP_TOL, BASE_TOLERANCE, BASE_TOLERANCE, BASE_TOLERANCE)

  /** Smooth parabola with min of 0.0 at P1, P2 */
  override def evaluateFitness(a: ParameterArray): Double =
    Math.pow(P1 - a.get(0).getValue, 2) + Math.pow(P2 - a.get(1).getValue, 2)
}


case object SINUSOIDAL extends ParabolaMinVariation {
  val errorTolerances = ErrorTolerances(
    0.009, RELAXED_TOL, 0.01, GLOB_SAMP_TOL, BASE_TOLERANCE, BASE_TOLERANCE, BASE_TOLERANCE)

  /** This version introduces a bit of sinusoidal noise.
    * @param a the position on the parabolic surface given the specified values of p1 and p2
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double =
    PARABOLA.evaluateFitness(a) + 0.5 * Math.cos((a.get(0).getValue - P1) * (a.get(1).getValue - P2)) - 0.5
}


case object ABS_SINUSOIDAL extends ParabolaMinVariation {
  val errorTolerances =
    ErrorTolerances(0.009, 0.0128, 0.01,
      GLOB_SAMP_TOL, RELAXED_TOL, RELAXED_TOL, BASE_TOLERANCE, BASE_TOLERANCE)

  /** This version introduces a bit of absolute value sinusoidal noise.
    * This means it will not be second order differentiable, making this type of search harder.
    * @param a the position on the parabolic surface given the specified values of p1 and p2
    * @return fitness value
    */
  override def evaluateFitness(a: ParameterArray): Double =
    PARABOLA.evaluateFitness(a) - 0.5 * Math.abs(Math.cos((a.get(0).getValue - P1) * (a.get(1).getValue - P2))) + 0.5
}


case object STEPPED extends ParabolaMinVariation {
  val errorTolerances = ErrorTolerances(0.009, RELAXED_TOL, BASE_TOLERANCE, GLOB_SAMP_TOL,
    RELAXED_TOL, RELAXED_TOL, BASE_TOLERANCE)

  /** This version introduces a bit of step function noise. */
  override def evaluateFitness(a: ParameterArray): Double =
    PARABOLA.evaluateFitness(a) +
      0.2 * Math.round(Math.abs((P1 - a.get(0).getValue) * Math.abs(P2 - a.get(1).getValue)))
}
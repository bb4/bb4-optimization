// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}

import scala.util.Random

/**
 * The bounded box that a swarm flies in, taken from the bounds of the initial guess.
 *
 * @param template             the initial guess; supplies the bounds, integer-only flags and redistribution functions
 * @param maxVelocityFraction  per-dimension speed limit as a fraction of that parameter's range
 */
private[particleswarm] final class SearchSpace(template: NumericParameterArray, maxVelocityFraction: Double) {

  val size: Int = template.size
  val lower: Array[Double] = Array.tabulate(size)(j => template.get(j).minValue)
  val upper: Array[Double] = Array.tabulate(size)(j => template.get(j).maxValue)
  private val range: Array[Double] = Array.tabulate(size)(j => upper(j) - lower(j))
  private val maxVelocity: Array[Double] = Array.tabulate(size)(j => maxVelocityFraction * range(j))

  /** @return the position of the initial guess */
  def initialGuessPosition: Array[Double] = positionOf(template)

  /** @return a position drawn uniformly from the box */
  def randomPosition(rnd: Random): Array[Double] =
    Array.tabulate(size)(j => lower(j) + rnd.nextDouble() * range(j))

  /** @return a velocity drawn uniformly from +/- `fraction` of each parameter's range */
  def randomVelocity(rnd: Random, fraction: Double): Array[Double] =
    Array.tabulate(size)(j => (2.0 * rnd.nextDouble() - 1.0) * fraction * range(j))

  /** @return `v` limited to the speed limit of dimension `j` */
  def clampVelocity(j: Int, v: Double): Double = math.max(-maxVelocity(j), math.min(maxVelocity(j), v))

  /** @return the values of the parameter array as plain doubles. */
  def positionOf(pa: ParameterArray): Array[Double] =
    Array.tabulate(pa.size)(j => pa.get(j).getValue)

  /**
   * Builds a new parameter array with the given values. The parameters keep their bounds and redistribution
   * function from the template. Integer parameters are rounded (their `setValue` would otherwise truncate).
   */
  def toParameterArray(values: Array[Double]): NumericParameterArray = {
    val parameters = IndexedSeq.tabulate(values.length) { j =>
      val parameter = template.get(j)
      parameter.setValue(if (parameter.isIntegerOnly) math.rint(values(j)) else values(j))
    }
    NumericParameterArray(parameters, template.numSteps, template.rnd)
  }
}

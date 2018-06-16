// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.{Direction, NumericParameterArray, ParameterArray}


/**
  * Utility class for maintaining the data vectors for the iteration when hill climbing
  * over a numerical parameter space.
  * @author Barry Becker
  */
class ImprovementIteration(params: NumericParameterArray, var oldGradient: Vector = null) {

  private var delta: Vector = params.asVector
  private var fitnessDelta: Vector = params.asVector
  var gradient: Vector = oldGradient

  if (oldGradient == null) {
    this.oldGradient = params.asVector

    // initialize the old gradient to the unit vector (any random direction will do)
    for (i <- 0 until params.size) {
      this.oldGradient.set(i, 1.0)
    }
    this.oldGradient = this.oldGradient.normalize
  }

  /** Compute the squares in one of the iteration directions and add it to the running sum.
    * @return the sum of squares in one of the iteration directions.
    */
  def incSumOfSqs(i: Int, sumOfSqs: Double, optimizee: Optimizee,
                  params: ParameterArray, testParams: ParameterArray): Double = {

    var fwdFitness: Double = 0
    var bwdFitness: Double = 0

    val p: Parameter = testParams.get(i)

    // increment forward.
    delta.set(i, p.incrementByEps(Direction.FORWARD))

    fwdFitness = findFitnessDelta(optimizee, params, testParams)

    // revert the increment
    p.incrementByEps(Direction.BACKWARD)
    // this checks the fitness on the other side (backwards).
    p.incrementByEps(Direction.BACKWARD)

    bwdFitness = findFitnessDelta(optimizee, params, testParams)

    fitnessDelta.set(i, fwdFitness - bwdFitness)
    sumOfSqs + (fitnessDelta.get(i) * fitnessDelta.get(i)) / (delta.get(i) * delta.get(i))
  }

  /** @param optimizee the thing being optimized
    * @param params the current parameters
    * @param testParams the new set of parameters being evaluated.
    * @return the incremental change in fitness
    */
  private def findFitnessDelta(optimizee: Optimizee, params: ParameterArray, testParams: ParameterArray): Double = {
    if (optimizee.evaluateByComparison) optimizee.compareFitness( testParams, params )
    else params.getFitness - optimizee.evaluateFitness( testParams )
  }

  /** Update gradient. Use EPS if the gradLength is 0. */
  def updateGradient(jumpSize: Double, gradLength: Double): Unit = {
    val gradLen: Double = if (gradLength ==0)  MathUtil.EPS_MEDIUM else gradLength
    for (i <- 0 until delta.size) {
      val denominator = delta.get(i) * gradLen
      gradient.set(i, jumpSize * fitnessDelta.get(i) / denominator)
    }
  }
}
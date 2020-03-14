// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.linear.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.{Direction, NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

/**
  * Utility class for maintaining the data vectors for the iteration when hill climbing
  * over a numerical parameter space.
  * @param params the params to improve.
  * @param oldGradient last direction of improvement
  * @author Barry Becker
  */
class ImprovementIteration(params: ParameterArrayWithFitness, var oldGradient: Vector = null) {

  private var delta: Vector = params.pa.asInstanceOf[NumericParameterArray].asVector
  private var fitnessDelta: Vector = params.pa.asInstanceOf[NumericParameterArray].asVector
  var gradient: Vector = params.pa.asInstanceOf[NumericParameterArray].asVector

  if (oldGradient == null) {
    this.oldGradient = params.pa.asInstanceOf[NumericParameterArray].asVector

    // initialize the old gradient to the unit vector (any random direction will do)
    for (i <- 0 until params.pa.size) {
      this.oldGradient.set(i, 1.0)
    }
    this.oldGradient = this.oldGradient.normalize
  }

  /** Compute the squares in one of the iteration directions and add it to the running sum.
    * @return the sum of squares in one of the iteration directions.
    */
  def incSumOfSqs(i: Int, optimizee: Optimizee): Double = {

    val p: Parameter = params.pa.get(i)

    // increment forward.
    delta = delta.set(i, p.getIncrementForDirection(Direction.FORWARD))
    val nparams = params.pa.asInstanceOf[NumericParameterArray]

    val forwardParams = nparams.incrementByEps(i, Direction.FORWARD)
    val fwdFitnessDelta = findFitnessDelta(optimizee, params, forwardParams)

    // this checks the fitness on the other side (backwards).
    //val backwardParams = nparams.incrementByEps(i, Direction.BACKWARD)
    //val bwdFitnessDelta = findFitnessDelta(optimizee, params, backwardParams) // reverse?

    fitnessDelta = fitnessDelta.set(i, fwdFitnessDelta) // - bwdFitnessDelta)
    println(s"fitDelta for $i = $fitnessDelta")
    val d = delta(i)
    assert(d != 0)
    (fwdFitnessDelta * fwdFitnessDelta) / (d * d)
  }

  /** @param optimizee the thing being optimized
    * @param params the current parameters
    * @param testParams the new set of parameters being evaluated.
    * @return the incremental change in fitness. More negative means bigger move toward goal.
    *         A positive value means it is moving away from goal.
    */
  private def findFitnessDelta(optimizee: Optimizee,
                               params: ParameterArrayWithFitness,
                               testParams: ParameterArray): Double = {
    if (optimizee.evaluateByComparison) optimizee.compareFitness( testParams, params.pa )
    else optimizee.evaluateFitness( testParams ) - params.fitness
  }

  /** Update gradient. Use EPS if the gradLength is 0. */
  def updateGradient(jumpSize: Double, gradLength: Double): Unit = {
    val gradLen: Double = if (gradLength == 0)  MathUtil.EPS_MEDIUM else gradLength
    for (i <- 0 until delta.size) {
      val denominator = delta(i) * gradLen
      gradient = gradient.set(i, -jumpSize * fitnessDelta(i) / denominator)
    }
    //// println("gradient = " + gradient + " after updating from fitnessDelta = " + fitnessDelta.toString())
  }
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import Improvement.JUMP_SIZE_EPS

object Improvement {
  private val JUMP_SIZE_EPS = 0.000000001
}

/**
  * Represents an incremental improvement for a ParameterArray. (unless the improvement is 0 or negative that is)
  * @param parameters the (hopefully) improved set of parameters
  * @param improvement the amount we improved compared to the last iteration (if any)
  * @param jumpSize the possibly revised jumpSize. Size of the iteration increment
  * @param gradient (optional) direction that we are currently moving in if any.
  * @author Barry Becker
  */
case class Improvement(parameters: ParameterArrayWithFitness,
                       improvement: Double, jumpSize: Double, gradient: Option[Vector] = None) {

  def improved: Boolean = improvement < 0 /* -fitnessEps*/ && jumpSize > JUMP_SIZE_EPS
}
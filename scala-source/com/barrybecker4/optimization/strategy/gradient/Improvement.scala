// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.math.linear.Vector
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

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

  def improved: Boolean = improvement < 0 && jumpSize > JUMP_SIZE_EPS
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * Represents an incremental improvement for a ParameterArray. (unless the improvement is 0 or negative that is)
  * @param parameters the (hopefully) improved set of parameters
  * @param improvement the amount we improved compared to the last iteration (if any)
  * @param newJumpSize the possibly revised jumpSize. Size of the iteration increment
  * @param gradient  direction that we are currently moving in.
  * @author Barry Becker
  */
case class Improvement(parameters: ParameterArray, improvement: Double, newJumpSize: Double, gradient: Vector = null) {

  /*
  override def toString: String = {
    val bldr = new StringBuilder
    bldr.append("Improvement = ")
      .append(improvement).append(", New jumpsize = ")
      .append(newJumpSize).append(", Gradient = ").append(gradient)
    bldr.toString
  }*/
}
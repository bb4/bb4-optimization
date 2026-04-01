// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}

/** Shared constants for coordinate / discrete hill-climbing helpers. */
package object gradient {

  private[gradient] def requireNumericParameterArray(pa: ParameterArray): NumericParameterArray =
    pa match {
      case n: NumericParameterArray => n
      case o =>
        throw new IllegalArgumentException(s"Expected NumericParameterArray, got ${o.getClass.getName}")
    }

  /** Termination threshold for step size in hill-climbing iterations. */
  val JUMP_SIZE_EPS: Double = 0.000000001

  /** If normalized dot(newGrad, oldGrad) is below this, shrink the step. */
  val GRADIENT_MIN_DOT_PRODUCT: Double = 0.3

  /** If normalized dot is above this, grow the step. */
  val GRADIENT_MAX_DOT_PRODUCT: Double = 0.98

  /** Max neighbor attempts per discrete improvement iteration. */
  val DISCRETE_MAX_TRIES: Int = 1000
}

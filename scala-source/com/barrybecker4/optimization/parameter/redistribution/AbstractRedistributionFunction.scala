// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.common.math.Range
import com.barrybecker4.common.math.function.InvertibleFunction


object AbstractRedistributionFunction {
  protected[redistribution] def verifyInRange(value: Double): Unit = {
    assert((value >= 0) && (value <= 1.0), "value, " + value + ", was outside the range 0 to 1.")
  }
}

/**
  * @author Barry Becker
  */
abstract class AbstractRedistributionFunction extends RedistributionFunction {

  /** the discretized redistribution function */
  protected var redistributionFunction: InvertibleFunction = _

  /**
    * @param value x value
    * @return the value for the function at position value.
    */
  override def getValue(value: Double): Double = {
    AbstractRedistributionFunction.verifyInRange(value)
    val newValue = redistributionFunction.getValue(value)
    AbstractRedistributionFunction.verifyInRange(newValue)
    newValue
  }

  /** @return the inverse of the specified value.*/
  override def getInverseFunctionValue(value: Double): Double = {
    AbstractRedistributionFunction.verifyInRange(value)
    val newValue = redistributionFunction.getInverseValue(value)
    AbstractRedistributionFunction.verifyInRange(newValue)
    newValue
  }

  override def getDomain = Range(0, 1.0)

  protected def initializeFunction(): Unit
}

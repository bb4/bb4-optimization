// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.common.math.Range
import com.barrybecker4.common.math.function.{Function, InvertibleFunction}
import RedistributionFunction.verifyInRange


object RedistributionFunction {

  protected[redistribution] def verifyInRange(value: Double): Unit = {
    assert((value >= 0) && (value <= 1.0), "value, " + value + ", was outside the range 0 to 1.")
  }
}

/**
  * Responsible for defining the probability distribution for selecting random parameter values.
  * Derived classes will define the different sorts of redistribution functions.
  * @author Barry Becker
  */
trait RedistributionFunction extends Function {

  /** the discretized redistribution function */
  protected var redistributionFunction: InvertibleFunction = _

  /** Given an x value, returns f(x)  (i.e. y)
    * Remaps values in the range [0, 1] -> [0, 1]
    * @param value x value
    * @return the value for the function at position value.
    */
  override def getValue(value: Double): Double = {
    verifyInRange(value)
    val newValue = redistributionFunction.getValue(value)
    verifyInRange(newValue)
    newValue
  }

  /** Given a y value (i.e. f(x)) return the corresponding x value.
    * Inverse of the above.
    * @param value y value to get inverse of
    * @return x for specified y
    */
  def getInverseFunctionValue(value: Double): Double = {
    verifyInRange(value)
    val newValue = redistributionFunction.getInverseValue(value)
    verifyInRange(newValue)
    newValue
  }

  override def getDomain = Range(0, 1.0)

  protected def initializeFunction(): Unit
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.common.math.function.Function


/**
  * Responsible for defining the probability distribution for selecting random parameter values.
  * Derived classes will define the different sorts of redistribution functions.
  * @author Barry Becker
  */
trait RedistributionFunction extends Function {

  /** Given an x value, returns f(x)  (i.e. y)
    * Remaps values in the range [0, 1] -> [0, 1]
    * @param value value to remap.
    * @return the remapped value.
    */
  override def getValue(value: Double): Double

  /** Given a y value (i.e. f(x)) return the corresponding x value.
    * Inverse of the above.
    * @param value y value to get inverse of
    * @return x for specified y
    */
  def getInverseFunctionValue(value: Double): Double
}

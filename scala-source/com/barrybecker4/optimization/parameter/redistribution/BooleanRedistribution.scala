// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import BooleanRedistribution._


object BooleanRedistribution {
  private val SPECIAL_VALUES = Array(0, 1)

  private def getSpecialValueProbs(percentTrue: Double) = Array(percentTrue, 1.0 - percentTrue)
}

/**
  * Boolean case of UniformRedistributionFunction.
  * @param percentTrue percent chance that the boolean parameter will have the value true. must be in range 0 to 1.0.
  * @author Barry Becker
  */
case class BooleanRedistribution(val percentTrue: Double)
  extends DiscreteRedistribution(2, SPECIAL_VALUES, getSpecialValueProbs(percentTrue)) {
}

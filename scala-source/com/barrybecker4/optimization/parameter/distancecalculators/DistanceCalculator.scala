// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * For different ways of calculating differences between parameter arrays.
  * @author Barry Becker
  */
trait DistanceCalculator {

  /** @return distance between parameter arrays */
  def calculateDistance(pa1: ParameterArray, pa2: ParameterArray): Double

}

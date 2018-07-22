// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * @author Barry Becker
  */
class MagnitudeDistanceCalculator extends DistanceCalculator {

  /**
    * The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two instances.
    * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
    *  - the length of the parameter array
    *  - the set of values in the parameter array.
    * Generally, the distance is greater the greater the number of parameters that are different.
    * The order of the values is not important.
    * @return the distance between this parameter array and another.
    */
  override def calculateDistance(pa1: ParameterArray, pa2: ParameterArray): Double = {
    val thisLength = pa1.size
    val thatLength = pa2.size
    var sum1: Double = 0
    var sum2: Double = 0
    for (i <- 0 until thisLength) {
      sum1 += pa1.get(i).getValue
    }
    for (i <- 0 until thatLength) {
      sum2 += pa2.get(i).getValue
    }
    Math.abs(sum1 - sum2)
  }
}

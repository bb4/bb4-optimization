// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution


/**
  * If you have just a purely uniform distribution you do not need to add any redistribution function
  * as that is the default. Use this function though, if you have uniform except for a few special values.
  * If the sum of all special value probabilities is equal to one, then no non-special values are ever selected.
  * @param numValues   number of values
  * @param discreteSpecialValues  certain values that are more likely to occur than other regular values.
  *                               (must be in increasing order)
  * @param discreteSpecialValueProbabilities sum of all special value probabilities must be less than or equal to one.
  * @author Barry Becker
  */
class DiscreteRedistribution(numValues: Int, discreteSpecialValues: Array[Int],
                             discreteSpecialValueProbabilities: Array[Double]) extends UniformRedistribution {

  val len: Int = discreteSpecialValues.length
  specialValues = new Array[Double](len)
  specialValueProbabilities = new Array[Double](len)

  for (i <- 0 until len) {
    assert(discreteSpecialValues(i) < numValues,
      " A discrete special value (" + discreteSpecialValues(i) + ") was >= " + numValues)
    specialValues(i) = discreteSpecialValues(i).toDouble / (numValues - 1).toDouble
    specialValueProbabilities(i) = discreteSpecialValueProbabilities(i)
  }
  initializeFunction()
}
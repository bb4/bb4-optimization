// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.function.{InvertibleFunction, PiecewiseFunction}
import RedistributionFunction.verifyInRange


/**
  * The default is a completely uniform distribution (all values having equal probability).
  * However, to get that, you do not actually need to specify any redistribution function,
  * so this class offers a twist on the concept. You can specify special values that you
  * want to have special probabilities in an otherwise uniform distribution.
  *
  * @param specialValues certain values that are more likely to occur than other regular values
  *                      (must be in increasing order)
  * @param specialValueProbabilities sum of all special value probabilities must be less than or equal to one.
  *                                  If equal to one, no non-special values are ever selected.
  * @author Barry Becker
  */
class UniformRedistribution(
    protected val specialValues: Array[Double],
    protected val specialValueProbabilities: Array[Double]
) extends RedistributionFunction {

  private val len = specialValues.length
  assert(len > 0,
    "must have at least one special value " + "(otherwise you could just use null for the redistribution function)")
  assert(len == specialValueProbabilities.length)

  protected val redistributionFunction: InvertibleFunction = buildPiecewiseFunction()

  private def buildPiecewiseFunction(): PiecewiseFunction = {
    val xValues = new Array[Double](2 * len + 2)
    val yValues = new Array[Double](2 * len + 2)
    var specialProbabilityTotal: Double = getSpecialProbTotal
    val ratio = 1.0 - specialProbabilityTotal
    // now compute the piecewise function values
    specialProbabilityTotal = 0
    xValues(0) = 0.0
    yValues(0) = 0.0
    var lastX = 0.0
    for (i <- 0 until len) {
      verifyInRange(specialValueProbabilities(i))
      specialProbabilityTotal += specialValueProbabilities(i)
      val i2 = 2 * i
      val specialValuesm1 = if (i == 0) 0.0
      else specialValues(i - 1)
      xValues(i2 + 1) = lastX + (specialValues(i) - specialValuesm1) * ratio
      yValues(i2 + 1) = specialValues(i)
      xValues(i2 + 2) = xValues(i2 + 1) + specialValueProbabilities(i)
      yValues(i2 + 2) = specialValues(i)
      if (i > 0) xValues(i2 + 1) += MathUtil.EPS
      lastX = xValues(i2 + 2)
    }
    if (len == 2) xValues(2 * len - 2) -= MathUtil.EPS_MEDIUM
    xValues(2 * len + 1) = 1.0
    yValues(2 * len + 1) = 1.0
    new PiecewiseFunction(xValues, yValues)
  }

  private def getSpecialProbTotal = {
    var specialProbabilityTotal: Double = 0
    for (i <- 0 until len) {
      verifyInRange(specialValues(i))
      if (i > 0) assert(specialValues(i) > specialValues(i - 1))
      specialProbabilityTotal += specialValueProbabilities(i)
    }
    //assert (specialProbabilityTotal < 1.0) :
    // "Sum of special probabilities is not less than one. It was " + specialProbabilityTotal;
    verifyInRange(specialProbabilityTotal)
    specialProbabilityTotal
  }
}

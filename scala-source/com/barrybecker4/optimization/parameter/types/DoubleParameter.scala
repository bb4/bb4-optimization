// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.GaussianRedistribution
import com.barrybecker4.optimization.parameter.redistribution.UniformRedistribution
import com.barrybecker4.optimization.parameter.ui.DoubleParameterWidget


object DoubleParameter {

  /** Approximate number of steps to take when marching across one of the parameter dimensions.
    * used to calculate the stepsize in a dimension direction.
    */
  private val NUM_STEPS = 30

  def createGaussianParameter(theVal: Double, minVal: Double, maxVal: Double, paramName: String,
                              normalizedMean: Double, stdDeviation: Double): DoubleParameter = {
    val param = new DoubleParameter(theVal, minVal, maxVal, paramName)
    param.setRedistributionFunction(new GaussianRedistribution(normalizedMean, stdDeviation))
    param
  }

  def createUniformParameter(theVal: Double, minVal: Double, maxVal: Double, paramName: String,
                             specialValues: Array[Double],
                             specialValueProbabilities: Array[Double]): DoubleParameter = {
    val param = new DoubleParameter(theVal, minVal, maxVal, paramName)
    param.setRedistributionFunction(new UniformRedistribution(specialValues, specialValueProbabilities))
    param
  }
}

/**
  * Represents a double (i.e. floating point) parameter to an algorithm
  * @param theVal       the initial or assign parameter value
  * @param minVal    the minimum value that this parameter is allowed to take on
  * @param maxVal    the maximum value that this parameter is allowed to take on
  * @param paramName of the parameter
  * @author Barry Becker
  */
class DoubleParameter(theVal: Double, minVal: Double, maxVal: Double, paramName: String)
  extends AbstractParameter(theVal, minVal, maxVal, paramName, false) {

  override def copy: Parameter = {
    val p = new DoubleParameter(getValue, minValue, maxValue, name)
    p.setRedistributionFunction(redistributionFunction)
    p
  }

  override def incrementByEps(direction: Direction): Double = {
    val increment = direction.multiplier * (maxValue - minValue) / DoubleParameter.NUM_STEPS
    val v = getValue
    if (v + increment > maxValue) {
      value = maxValue
      0
    }
    else if (v + increment < minValue) {
      value = minValue
      0
    }
    else {
      value = v + increment
      increment
    }
  }

  override def getNaturalValue: Any = this.getValue
  override def getType: Class[_] = classOf[Float]
  override def createWidget(listener: ParameterChangeListener) = new DoubleParameterWidget(this, listener)
}
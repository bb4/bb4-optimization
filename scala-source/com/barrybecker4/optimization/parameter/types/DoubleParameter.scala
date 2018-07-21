// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.{GaussianRedistribution, RedistributionFunction, UniformRedistribution}
import com.barrybecker4.optimization.parameter.ui.DoubleParameterWidget

import scala.util.Random


object DoubleParameter {

  /** Approximate number of steps to take when marching across one of the parameter dimensions.
    * used to calculate the stepsize in a dimension direction.
    */
  private val NUM_STEPS = 1000

  def createGaussianParameter(value: Double, minValue: Double, maxValue: Double, paramName: String,
                              normalizedMean: Double, stdDeviation: Double): DoubleParameter = {
    DoubleParameter(value, minValue, maxValue, paramName,
      Some(new GaussianRedistribution(normalizedMean, stdDeviation)))
  }

  def createUniformParameter(theVal: Double, minVal: Double, maxVal: Double, paramName: String,
                             specialValues: Array[Double],
                             specialValueProbabilities: Array[Double]): DoubleParameter = {
    DoubleParameter(theVal, minVal, maxVal, paramName,
      Some(new UniformRedistribution(specialValues, specialValueProbabilities)))
  }
}

/**
  * Represents a double (i.e. floating point) parameter to an algorithm
  * @param value       the initial or assign parameter value
  * @param minValue    the minimum value that this parameter is allowed to take on
  * @param maxValue    the maximum value that this parameter is allowed to take on
  * @param name of the parameter
  * @author Barry Becker
  */
case class DoubleParameter(value: Double, minValue: Double, maxValue: Double, name: String,
                           redistFunc: Option[RedistributionFunction] = None)
  extends AbstractParameter(value, minValue, maxValue, name, redistFunc) {

  override def copy: Parameter =
    DoubleParameter(getValue, minValue, maxValue, name, redistFunc)

  override def isIntegerOnly: Boolean = false

  override def randomizeValue(rand: Random): DoubleParameter =
    new DoubleParameter(getRandomValue(rand), minValue, maxValue, name, redistFunc)

  override def tweakValue(r: Double, rand: Random): DoubleParameter =
    DoubleParameter(tweakNumericValue(value, r, rand), minValue, maxValue, name, redistFunc)

  override def getIncrementForDirection(direction: Direction): Double = {
    val increment = direction.multiplier * (maxValue - minValue) / DoubleParameter.NUM_STEPS
    if (value + increment > maxValue) maxValue
    else if (value + increment < minValue) 0
    else increment
  }

  override def incrementByEps(direction: Direction): DoubleParameter = {
    val increment = getIncrementForDirection(direction)
    DoubleParameter(value + increment, minValue, maxValue, name, redistFunc)
  }

  override def setValue(value: Double): DoubleParameter = {
    val newVal = findNewValue(value)
    new DoubleParameter(newVal, minValue, maxValue, name, redistFunc)
  }

  override def getNaturalValue: Any = this.getValue
  override def getType: Class[_] = classOf[Float]
  override def createWidget(listener: ParameterChangeListener) = new DoubleParameterWidget(this, listener)
}
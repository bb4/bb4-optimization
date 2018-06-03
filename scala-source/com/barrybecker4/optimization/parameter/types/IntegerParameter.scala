// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.DiscreteRedistribution
import com.barrybecker4.optimization.parameter.ui.{DoubleParameterWidget, ParameterWidget}

import scala.util.Random


/**
  * Represents an integer parameter in an algorithm
  * @author Barry Becker
  */
object IntegerParameter {

  def createDiscreteParameter(theVal: Int, minVal: Int, maxVal: Int, paramName: String,
              discreteSpecialValues: Array[Int], specialValueProbabilities: Array[Double]): IntegerParameter = {
    val param = new IntegerParameter(theVal, minVal, maxVal, paramName)
    val numValues = maxVal - minVal + 1
    param.setRedistributionFunction(DiscreteRedistribution(numValues, discreteSpecialValues, specialValueProbabilities))
    param
  }
}

class IntegerParameter(theVal: Int, minVal: Int, maxVal: Int, paramName: String)
    extends AbstractParameter(theVal.toDouble, minVal.toDouble, maxVal.toDouble, paramName, true) {
  override def copy: Parameter = {
    val p = new IntegerParameter(getValue.round.toInt, minValue.toInt, maxValue.toInt, name)
    p.setRedistributionFunction(redistributionFunction)
    p
  }

  override def randomizeValue(rand: Random): Unit = {
    value = minValue + rand.nextDouble * (range + 1.0)
  }

  override def tweakValue(r: Double, rand: Random): Unit = {
    if (isOrdered) super.tweakValue(r, rand)
    else {
      val rr = rand.nextDouble
      if (rr < r) { // if not ordered, then just randomize with probability r
        randomizeValue(rand)
      }
    }
  }

  protected def isOrdered = true

  /** increments the parameter based on the number of steps to get from one end of the range to the other.
    * If we are already at the max end of the range, then we can only move in the other direction if at all.
    * @param direction 1 for forward, -1 for backward.
    * @return the size of the increment taken
    */
  override def incrementByEps(direction: Direction): Double = {
    val increment = direction.multiplier
    value = getValue + increment
    increment
  }

  override def setValue(value: Double): Unit = {
    this.value = value
    // if there is a redistribution function, we need to apply its inverse.
    if (redistributionFunction != null) {
      val v = (value - minValue) / (range + 1.0)
      this.value = minValue + (range + 1.0) * redistributionFunction.getInverseFunctionValue(v)
    }
  }

  override def getValue: Double = {
    var value = this.value
    if (redistributionFunction != null) {
      val v = (this.value - minValue) / (range + 1.0)
      val rv = redistributionFunction.getValue(v)
      value = rv * (range + (1.0 - MathUtil.EPS)) + minValue
    }
    value.round
  }

  override def getNaturalValue: Any = this.getValue.round
  override def getType: Class[_] = classOf[Int]
  override def createWidget(listener: ParameterChangeListener): ParameterWidget =
    new DoubleParameterWidget(this, listener)
}
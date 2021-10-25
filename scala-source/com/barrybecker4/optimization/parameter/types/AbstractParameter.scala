// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.redistribution.RedistributionFunction

import scala.util.Random


/**
  * Represents a general parameter to an algorithm.
  * setValue, tweakValue and randomizeValue should return new instances.
  * @param value  the initial or assign parameter value
  * @param minValue  the minimum value that this parameter is allowed to take on
  * @param maxValue  the maximum value that this parameter is allowed to take on
  * @param name of the parameter
  * @param redistributionFunction This optional function redistributes the normally uniform
  * parameter distribution into something potentially completely different
  * like a gaussian, or one where specific values have higher probability than others.
  * @author Barry Becker
  */
abstract class AbstractParameter(value: Double,
          minValue: Double, maxValue: Double,
          name: String,
          redistributionFunction: Option[RedistributionFunction] = None)
  extends Parameter {

  val range: Double = maxValue - minValue

  def this(value: Double, minVal: Double, maxVal: Double, paramName: String) = {
    this(value, minVal, maxVal, paramName, None)
  }

  protected def tweakNumericValue(v: Double, r: Double, rand: Random): Double = {
    assert(Math.abs(r) <= 1.5)
    if (r == 0) return v // no change in the param.
    val change = rand.nextGaussian() * r * range
    var newValue = v + change
    if (newValue > maxValue) newValue = maxValue
    else if (newValue < minValue) newValue = minValue
    newValue
  }

  override def getIncrementForDirection(direction: Direction): Double = direction.multiplier

  protected def getRandomValue(rand: Random): Double =
    minValue + rand.nextDouble() * range

  protected def findNewValue(proposedNewValue: Double): Double = {
    validateRange(proposedNewValue)

    // if there is a redistribution function, we need to apply its inverse.
    if (redistributionFunction.isDefined) {
      val v = (proposedNewValue - minValue) / range
      minValue + range * redistributionFunction.get.getInverseFunctionValue(v)
    } else proposedNewValue
  }

  override def toString: String = {
    val sa = new StringBuilder(name)
    sa.append(" = ")
    sa.append(FormatUtil.formatNumber(getValue))
    sa.append(" [")
    sa.append(FormatUtil.formatNumber(minValue))
    sa.append(", ")
    sa.append(FormatUtil.formatNumber(maxValue))
    sa.append(']')
    if (redistributionFunction.isDefined)
      sa.append(" redistributionFunction=").append(redistributionFunction.get)
    sa.toString
  }

  override def getType: Class[_] =
    if (isIntegerOnly) classOf[Int]
    else classOf[Float]

  override def getValue: Double = {
    val retValue =
      if (redistributionFunction.isDefined) {
        var v = (this.value - minValue) / range
        v = redistributionFunction.get.getValue(v)
        v * range + minValue
      } else this.value
    validateRange(retValue)
    retValue
  }

  private def validateRange(value: Double): Unit = {
    assert(value >= minValue && value <= maxValue,
      "Value " + value + " outside range [" + minValue + ", " + maxValue + "] for parameter " + name)
  }
}
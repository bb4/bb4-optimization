// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.{DiscreteRedistribution, RedistributionFunction}
import com.barrybecker4.optimization.parameter.ui.{DoubleParameterWidget, ParameterWidget}
import scala.util.Random

/**
  * Base class for all parameters that are integer based
  * @author Barry Becker
  */
abstract class AbstractIntParameter(theVal: Int, minVal: Int, maxVal: Int, paramName: String,
                       redisFunc: Option[RedistributionFunction] = None)
  extends AbstractParameter(theVal.toDouble, minVal.toDouble, maxVal.toDouble, paramName, redisFunc) {

  protected def getRandomVal(rand: Random): Int =
    (minVal + rand.nextDouble() * range).round.toInt

  protected def tweakIntValue(radius: Double, rand: Random): Int = {
      if (isOrdered) super.tweakNumericValue(theVal, radius, rand).round.toInt
      else if (rand.nextDouble() < radius)  // if not ordered, then just randomize with probability r
        getRandomVal(rand)
      else theVal
  }

  override def isIntegerOnly: Boolean = true
  protected def isOrdered = true

  /** Increments the parameter based on the number of steps to get from one end of the range to the other.
    * If we are already at the max end of the range, then we can only move in the other direction if at all.
    * @param direction forward or backward.
    * @return the size of the increment taken
    */
  override def incrementByEps(direction: Direction): IntegerParameter = {
    new IntegerParameter((getValue + direction.multiplier).toInt, minVal, maxVal, paramName, redisFunc)
  }

  override def getValue: Double = {
    if (redisFunc.isDefined) {
      val v = (theVal - minVal) / (range + 1.0)
      val rv = redisFunc.get.getValue(v)
      (rv * (range + (1.0 - MathUtil.EPS)) + minVal).round.toDouble
    } else theVal
  }

  override def getNaturalValue: Any = this.getValue.round
  override def getType: Class[_] = classOf[Int]
  override def createWidget(listener: ParameterChangeListener): ParameterWidget =
    new DoubleParameterWidget(this, listener)
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.Direction
import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.{DiscreteRedistribution, RedistributionFunction}
import com.barrybecker4.optimization.parameter.ui.{DoubleParameterWidget, ParameterWidget}
import scala.util.Random


/**
  * Represents an integer parameter in an algorithm
  * @author Barry Becker
  */
object IntegerParameter {

  def createDiscreteParameter(theVal: Int, minVal: Int, maxVal: Int, name: String,
              discreteSpecialValues: Array[Int], specialValueProbabilities: Array[Double]): IntegerParameter = {
    val numValues = maxVal - minVal + 1
    IntegerParameter(theVal, minVal, maxVal, name,
      Some(DiscreteRedistribution(numValues, discreteSpecialValues, specialValueProbabilities)))
  }
}

case class IntegerParameter(theVal: Double, minValue: Double, maxValue: Double, name: String,
                       redisFunc: Option[RedistributionFunction] = None)
    extends AbstractIntParameter(theVal.toInt, minValue.toInt, maxValue.toInt, name, redisFunc) {

  override def copy: IntegerParameter =
    IntegerParameter(getValue.round.toInt, minValue, maxValue, name, redisFunc)

  override def randomizeValue(rand: Random): IntegerParameter =
    IntegerParameter(getRandomVal(rand), minValue, maxValue, name, redisFunc)

  override def tweakValue(r: Double, rand: Random): IntegerParameter =
    IntegerParameter(tweakIntValue(r, rand), minValue, maxValue, name, redisFunc)

  /** increments the parameter based on the number of steps to get from one end of the range to the other.
    * If we are already at the max end of the range, then we can only move in the other direction if at all.
    * @param direction forward or backward.
    * @return the size of the increment taken
    */
  override def incrementByEps(direction: Direction): IntegerParameter =
    new IntegerParameter((getValue + direction.multiplier).toInt, minValue, maxValue, name, redisFunc)

  override def setValue(value: Double): IntegerParameter = {
    val retValue =
      if (redisFunc.isDefined) {
        val v = (value - minValue) / (range + 1.0)
        minValue + (range + 1.0) * redisFunc.get.getInverseFunctionValue(v)
      } else value
    new IntegerParameter(retValue.toInt, minValue, maxValue, name, redisFunc)
  }

  override def getValue: Double = {
    if (redisFunc.isDefined) {
      val v = (theVal - minValue) / (range + 1.0)
      val rv = redisFunc.get.getValue(v)
      (rv * (range + (1.0 - MathUtil.EPS)) + minValue).round
    } else theVal
  }

  override def getNaturalValue: Any = this.getValue.round
  override def getType: Class[_] = classOf[Int]
  override def createWidget(listener: ParameterChangeListener): ParameterWidget =
    new DoubleParameterWidget(this, listener)
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.redistribution.RedistributionFunction
import scala.util.Random


/**
  * Represents a general parameter to an algorithm
  * @param theVal  the initial or assign parameter value
  * @param minVal  the minimum value that this parameter is allowed to take on
  * @param maxVal  the maximum value that this parameter is allowed to take on
  * @param paramName of the parameter
  * @author Barry Becker
  */
abstract class AbstractParameter(val theVal: Double, val minVal: Double, val maxVal: Double, val paramName: String)
    extends Parameter {

  private var integerOnly = false
  protected var value: Double = theVal
  protected var minValue: Double = minVal
  private var maxValue: Double = maxVal
  private var range: Double = maxVal - minVal
  protected var redistributionFunction: RedistributionFunction = _

  def this(theVal: Double, minVal: Double, maxVal: Double, paramName: String, intOnly: Boolean) {
    this(theVal, minVal, maxVal, paramName)
    integerOnly = intOnly
  }

  override def isIntegerOnly: Boolean = integerOnly

  /** Tweak the value of this parameter a little. If r is big, you may be tweaking it a lot.
    * @param r the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *          r is relative to each parameter range (in other words scaled by it).
    */
  override def tweakValue(r: Double, rand: Random): Unit = {
    assert(Math.abs(r) <= 1.5)
    if (r == 0) return // no change in the param.
    val change = rand.nextGaussian * r * getRange
    value += change
    if (value > getMaxValue) value = getMaxValue
    else if (value < getMinValue) value = getMinValue
    setValue(value)
  }

  override def randomizeValue(rand: Random): Unit = {
    setValue(getMinValue + rand.nextDouble * getRange)
  }

  override def toString: String = {
    val sa = new StringBuilder(getName)
    sa.append(" = ")
    sa.append(FormatUtil.formatNumber(getValue))
    sa.append(" [")
    sa.append(FormatUtil.formatNumber(getMinValue))
    sa.append(", ")
    sa.append(FormatUtil.formatNumber(getMaxValue))
    sa.append(']')
    if (redistributionFunction != null) sa.append(" redistributionFunction=").append(redistributionFunction)
    sa.toString
  }

  override def getType: Class[_] = if (isIntegerOnly) classOf[Int] // Integer.TYPE;  //int.class;
  else classOf[Float] // Float.TYPE; //  float.class;

  override def setValue(value: Double): Unit = {
    validateRange(value)
    this.value = value
    // if there is a redistribution function, we need to apply its inverse.
    if (redistributionFunction != null) {
      val v = (value - minValue) / getRange
      this.value = minValue + getRange * redistributionFunction.getInverseFunctionValue(v)
    }
  }

  override def getValue: Double = {
    var value = this.value
    if (redistributionFunction != null) {
      var v = (this.value - minValue) / getRange
      v = redistributionFunction.getValue(v)
      value = v * getRange + minValue
    }
    validateRange(value)
    value
  }

  override def getMinValue: Double = minValue
  override def getMaxValue: Double = maxValue
  override def getRange: Double = range
  override def getName: String = paramName

  override def setRedistributionFunction(func: RedistributionFunction): Unit = {
    redistributionFunction = func
  }

  private def validateRange(value: Double): Unit = {
    assert(value >= minValue && value <= maxValue,
      "Value " + value + " outside range [" + minValue + ", " + maxValue + "] for parameter " + getName)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AbstractParameter]

  override def equals(other: Any): Boolean = other match {
    case that: AbstractParameter =>
      (that canEqual this) &&
        integerOnly == that.integerOnly &&
        value == that.value &&
        minValue == that.minValue &&
        maxValue == that.maxValue
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(integerOnly, value, minValue, maxValue)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
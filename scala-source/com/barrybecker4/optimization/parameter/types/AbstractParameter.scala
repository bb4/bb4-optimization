// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.redistribution.RedistributionFunction
import scala.util.Random


/**
  * TODO: this needs to be made an immutable case class. Create a new instance when changing the value
  * Represents a general parameter to an algorithm.
  * tweakValue and randomizeValue should return new instances.
  * @param theVal  the initial or assign parameter value
  * @param minValue  the minimum value that this parameter is allowed to take on
  * @param maxValue  the maximum value that this parameter is allowed to take on
  * @param name of the parameter
  * @author Barry Becker
  */
abstract class AbstractParameter(val theVal: Double,
                                 val minValue: Double, val maxValue: Double,
                                 val name: String, val isIntegerOnly: Boolean) extends Parameter {

  protected var value: Double = theVal
  val range: Double = maxValue - minValue
  protected var redistributionFunction: RedistributionFunction = _

  def this(theVal: Double, minVal: Double, maxVal: Double, paramName: String) {
    this(theVal, minVal, maxVal, paramName, false)
  }

  /** Tweak the value of this parameter a little. If r is big, you may be tweaking it a lot.
    * @param r the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *    r is relative to each parameter range (in other words scaled by it).
    */
  override def tweakValue(r: Double, rand: Random): Unit = {
    assert(Math.abs(r) <= 1.5)
    if (r == 0) return // no change in the param.
    val change = rand.nextGaussian * r * range
    value += change
    if (value > maxValue) value = maxValue
    else if (value < minValue) value = minValue
    setValue(value)
  }

  override def randomizeValue(rand: Random): Unit = {
    setValue(minValue + rand.nextDouble * range)
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
      val v = (value - minValue) / range
      this.value = minValue + range * redistributionFunction.getInverseFunctionValue(v)
    }
  }

  override def getValue: Double = {
    var value = this.value
    if (redistributionFunction != null) {
      var v = (this.value - minValue) / range
      v = redistributionFunction.getValue(v)
      value = v * range + minValue
    }
    validateRange(value)
    value
  }

  override def setRedistributionFunction(func: RedistributionFunction): Unit = {
    redistributionFunction = func
  }

  private def validateRange(value: Double): Unit = {
    assert(value >= minValue && value <= maxValue,
      "Value " + value + " outside range [" + minValue + ", " + maxValue + "] for parameter " + name)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AbstractParameter]

  override def equals(other: Any): Boolean = other match {
    case that: AbstractParameter =>
      (that canEqual this) &&
        isIntegerOnly == that.isIntegerOnly &&
        value == that.value &&
        minValue == that.minValue &&
        maxValue == that.maxValue
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(isIntegerOnly, value, minValue, maxValue)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
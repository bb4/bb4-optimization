// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.ui.StringParameterWidget


/**
  * Represents a general parameter to an algorithm
  * @author Barry Becker
  */
class StringParameter(index: Int, values: Array[String], paramName: String)
    extends IntegerParameter(index, 0, values.length - 1, paramName) {

  def this(theVal: Enum[_], enumValues: Array[Enum[_]], paramName: String) {
    this(theVal.ordinal(), enumValues.map(_.toString), paramName)
  }

  override def copy: Parameter = {
    val p = new StringParameter(getValue.toInt, values, getName)
    p.setRedistributionFunction(redistributionFunction)
    p
  }

  override def getNaturalValue: Any = values(getValue.toInt)
  def getStringValues: Array[String] = values
  override protected def isOrdered = false
  override def getType: Class[_] = classOf[String]
  override def createWidget(listener: ParameterChangeListener) = new StringParameterWidget(this, listener)
}
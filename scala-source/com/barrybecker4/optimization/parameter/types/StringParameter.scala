// Copyright by Barry G. Becker, 2000 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.RedistributionFunction
import com.barrybecker4.optimization.parameter.ui.StringParameterWidget
import scala.util.Random


/**
  * Represents a general parameter to an algorithm
  * @author Barry Becker
  */
case class StringParameter(index: Int, values: Array[String], name: String,
                           redistFunc: Option[RedistributionFunction] = None)
    extends AbstractIntParameter(index, 0, values.length - 1, name, redistFunc) {

  def this(theVal: Enum[_], enumValues: Array[Enum[_]], paramName: String) {
    this(theVal.ordinal(), enumValues.map(_.toString), paramName, None)
  }

  override def minValue: Double = 0
  override def maxValue: Double = values.length - 1

  override def copy: StringParameter =
    StringParameter(getValue.toInt, values, name, redistFunc)

  override def randomizeValue(rand: Random): StringParameter =
    StringParameter(getRandomVal(rand), values, name, redistFunc)

  override def tweakValue(r: Double, rand: Random): StringParameter =
    StringParameter(tweakIntValue(r, rand), values, name, redistFunc)

  override def getNaturalValue: Any =
    values(getValue.toInt)

  def getStringValues: Array[String] = values
  override protected def isOrdered = false
  override def getType: Class[_] = classOf[String]
  override def createWidget(listener: ParameterChangeListener) = new StringParameterWidget(this, listener)
}
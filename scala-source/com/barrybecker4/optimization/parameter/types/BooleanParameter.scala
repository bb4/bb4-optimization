// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.BooleanRedistribution
import com.barrybecker4.optimization.parameter.ui.BooleanParameterWidget
import com.barrybecker4.optimization.parameter.ui.ParameterWidget

import scala.util.Random


object BooleanParameter {
  def createSkewedParameter(value: Boolean, paramName: String, percentTrue: Double): BooleanParameter = {
    new BooleanParameter(value, paramName, Some(new BooleanRedistribution(percentTrue)))
  }
}

/**
  * Represents a boolean parameter to an algorithm.
  * @author Barry Becker
  */
case class BooleanParameter(bValue: Boolean, name: String, redistFunc: Option[BooleanRedistribution] = None)
  extends AbstractIntParameter(if (bValue) 1 else 0, 0, 1, name, redistFunc) {

  override def minValue: Double = 0
  override def maxValue: Double = 1

  override def copy: BooleanParameter =
    BooleanParameter(getNaturalValue.asInstanceOf[Boolean], name, redistFunc)

  override def randomizeValue(rand: Random): BooleanParameter =
    new BooleanParameter(if (getRandomVal(rand) == 1.0) true else false, name,  redistFunc)

  override def tweakValue(r: Double, rand: Random): BooleanParameter =
    new BooleanParameter(if (tweakIntValue(r, rand) == 0) true else false, name, redistFunc)

  /** @return true if getValue is odd. */
  override def getNaturalValue: Any = (getValue.toInt % 2) == 1

  override protected def isOrdered = false

  override def getType: Class[_] = classOf[Boolean]

  override def createWidget(listener: ParameterChangeListener): ParameterWidget =
    new BooleanParameterWidget(this, listener)
}
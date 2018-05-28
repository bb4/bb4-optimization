// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.optimization.parameter.ParameterChangeListener
import com.barrybecker4.optimization.parameter.redistribution.BooleanRedistribution
import com.barrybecker4.optimization.parameter.ui.BooleanParameterWidget
import com.barrybecker4.optimization.parameter.ui.ParameterWidget


/**
  * Represents a boolean parameter to an algorithm.
  *
  * @author Barry Becker
  */
object BooleanParameter {
  def createSkewedParameter(value: Boolean, paramName: String, percentTrue: Double): BooleanParameter = {
    val param = new BooleanParameter(value, paramName)
    param.setRedistributionFunction(new BooleanRedistribution(percentTrue))
    param
  }
}

class BooleanParameter(theVal: Boolean, paramName: String)
  extends IntegerParameter(if (theVal) 1 else 0, 0, 1, paramName) {
  override def copy: Parameter = {
    val p = new BooleanParameter(getNaturalValue.asInstanceOf[Boolean], getName)
    p.setRedistributionFunction(redistributionFunction)
    p
  }

  /**
    * @return true if getValue is odd.
    */
  override def getNaturalValue: Any = (getValue.toInt % 2) == 1

  override protected def isOrdered = false

  override def getType: Class[_] = classOf[Boolean]

  override def createWidget(listener: ParameterChangeListener): ParameterWidget =
    new BooleanParameterWidget(this, listener)
}

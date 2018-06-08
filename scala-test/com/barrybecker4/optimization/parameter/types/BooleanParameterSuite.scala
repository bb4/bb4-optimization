// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class BooleanParameterSuite extends ParameterSuite[Boolean] {

  override protected def createParameter = new BooleanParameter(true, "boolean param")

  override protected def expectedIsIntegerOnly = true

  override protected def expectedMinValue = 0.0

  override protected def expectedMaxValue = 1.0

  override protected def expectedRange = 1.0

  override protected def expectedValue = 1.0

  override protected def expectedNaturalValue: Boolean = true

  override protected def expectedForwardEpsChange = 2.0

  override protected def expectedBackwardEpsChange = 0.0

  override protected def expectedTweakedValues: Array[Boolean] =
    Array[Boolean](true, true, true, true, false)
}

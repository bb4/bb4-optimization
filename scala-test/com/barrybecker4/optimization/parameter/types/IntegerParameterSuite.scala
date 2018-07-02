// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class IntegerParameterSuite extends ParameterSuite[Integer] {

  override protected def createParameter = new IntegerParameter(4, 3, 11, "integer param")

  override protected def expectedIsIntegerOnly = true

  override protected def expectedMinValue = 3.0

  override protected def expectedMaxValue = 11.0

  override protected def expectedRange = 8.0

  override protected def expectedValue = 4.0

  override protected def expectedNaturalValue = 4L

  override protected def expectedForwardEpsChange = 5.0

  override protected def expectedBackwardEpsChange = 3.0

  override protected def expectedTweakedValues: Array[Integer] =
    Array[Integer](4, 4, 3, 3, 3) //4, 5, 7, 3, 3)
}

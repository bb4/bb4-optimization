// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class IntegerParameterSuite extends ParameterSuite[Int] {

  override protected def createParameter = new IntegerParameter(5, 3, 11, "integer param")

  override protected def expectedIsIntegerOnly = true

  override protected def expectedMinValue = 3.0

  override protected def expectedMaxValue = 11.0

  override protected def expectedRange = 8.0

  override protected def expectedValue = 5.0

  override protected def expectedNaturalValue = 5L

  override protected def expectedForwardEpsChange = 6.0

  override protected def expectedBackwardEpsChange = 4.0

  override protected def expectedTweakedValues: Array[Int] =
    Array[Int](5, 4, 3, 3, 3) //4, 5, 7, 3, 3)

  override protected def expectedRandomValues: Array[Int] =
    Array[Int](8, 6)
}

// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class DoubleParameterSuite extends ParameterSuite[Double] {

  override protected def createParameter = DoubleParameter(2.0, 0.0, 5.0, "double param")

  override protected def expectedMinValue = 0.0

  override protected def expectedMaxValue = 5.0

  override protected def expectedRange = 5.0

  override protected def expectedValue = 2.0

  override protected def expectedNaturalValue = 2.0

  override protected def expectedForwardEpsChange = 2.16666666667

  override protected def expectedBackwardEpsChange = 1.8333333333333333

  override protected def expectedTweakedValues: Array[Double] =
    Array(2.1561581040188953, 1.8520668005154652, 0.0, 0.0, 0.0)

  override protected def expectedRandomValues: Array[Double] =
    Array[Double](3.6543909535164545, 2.0504040574610083)
}

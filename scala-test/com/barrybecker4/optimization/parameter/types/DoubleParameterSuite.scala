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
    Array(
      2.1561581040188953, 1.8520668005154652, 0.7608389175707564, 0.0, 0.0, 0.0,
      0.0, 0.3259052193412455, 0.0, 2.013966520937554
    )

  override protected def expectedRandomValues: Array[Double] =
    Array[Double](
      3.6543909535164545, 2.0504040574610083, 1.0385742065485855, 1.6635852797975559,
      4.838779547120604, 0.030585911328806503, 4.818523985116038, 4.699326943909549,
      4.735974588315969, 4.6854107444798485
    )
}

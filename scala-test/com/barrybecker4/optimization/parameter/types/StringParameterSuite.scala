// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class StringParameterSuite extends ParameterSuite[String] {

  override protected def createParameter =
    StringParameter(2, Array("foo", "bar", "baz", "abc", "bcd", "barry", "becker"), "integer param")

  override protected def expectedIsIntegerOnly = true

  override protected def expectedMinValue = 0.0

  override protected def expectedMaxValue = 6.0

  override protected def expectedRange = 6.0

  override protected def expectedValue = 2.0

  override protected def expectedNaturalValue = "baz"

  override protected def expectedForwardEpsChange = 3.0

  override protected def expectedBackwardEpsChange = 1.0

  override protected def expectedTweakedValues: Array[String] =
    Array("baz", "baz", "baz", "becker", "becker", "becker", "becker", "baz", "abc", "barry")

  override protected def expectedRandomValues: Array[String] =
    Array[String]("bcd", "baz", "bar", "baz", "becker", "foo", "becker", "becker", "becker", "becker")
}
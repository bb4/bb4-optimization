// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

/**
  * @author Barry Becker
  */
class BooleanParameterSuite extends ParameterSuite[Boolean] {

  override protected def createParameter = BooleanParameter(bValue = true, "boolean param")
  override protected def createOtherParameter = BooleanParameter(bValue = false, "boolean param")

  override protected def expectedIsIntegerOnly = true

  override protected def expectedMinValue = 0.0

  override protected def expectedMaxValue = 1.0

  override protected def expectedRange = 1.0

  override protected def expectedValue = 1.0

  override protected def expectedNaturalValue: Boolean = true

  // true is already the upper bound (1). A forward step must stay inside [0, 1].
  override protected def expectedForwardEpsChange = 1.0

  override protected def expectedBackwardEpsChange = 0.0

  override protected def expectedTweakedValues: Array[Boolean] =
    Array[Boolean](false, true, false, false, false, true, false, true, false, false)

  override protected def expectedRandomValues: Array[Boolean] =
    Array[Boolean](true, false, false, false, true, false, true, true, true, true)

  /** Regression: parent ParameterWidget ctor calls addChildren before subclass fields init. */
  test("createWidget does not NPE") {
    val param = createParameter
    val widget = param.createWidget(_ => ())
    assert(widget != null)
  }
}

// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.Direction
import org.junit.Assert.assertEquals
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.util.Random

/**
  * Base class for all parameter test classes
  * @author Barry Becker
  */
abstract class ParameterSuite[T] extends FunSuite with BeforeAndAfter {
  /** instance under test */
  protected var parameter: Parameter = _
  protected var rand: Random = _

  before {
    rand = new Random(1)
    parameter = createParameter
  }

  protected def createParameter: Parameter
  // used in equality tests
  protected def createOtherParameter: Parameter

  test("IsIntegerOnly") {
    assertResult(expectedIsIntegerOnly) { parameter.isIntegerOnly }
  }

  test("GetMinValue(") {
    assertResult( expectedMinValue) { parameter.minValue }
  }

  test("GetMaxValue") {
    assertResult(expectedMaxValue) { parameter.maxValue }
  }

  test("GetRange") {
    assertResult(expectedRange) { parameter.range }
  }

  test("Value") {
    assertResult(expectedValue) { parameter.getValue }
  }

  test("NaturalValue(") {
    assertResult(expectedNaturalValue) { parameter.getNaturalValue }
  }

  test("IncrementByEpsForward(") {
    val newParam = parameter.incrementByEps(Direction.FORWARD)
    assertEquals("Unexpected eps forward", expectedForwardEpsChange, newParam.getValue, MathUtil.EPS_MEDIUM)
  }

  test("IncrementByEpsBackward") {
    val newParam = parameter.incrementByEps(Direction.BACKWARD)
    assertEquals("Unexpected eps backward", expectedBackwardEpsChange, newParam.getValue, MathUtil.EPS_MEDIUM)
  }

  test("tweaked values") {
    val radii = Array[Double](0.02, 0.1, 0.2, 0.4, 0.6, 0.8, 1.0, 1.1, 1.2, 1.4)

    var newParam = parameter
    val actResult = for(r <- radii) yield {
      newParam = newParam.tweakValue(r, rand)
      newParam.getNaturalValue
    }
    assertResult(expectedTweakedValues) { actResult }
  }

  test("random values") {
    var rParam = parameter

    val actResult = for (i <- 0 until 10) yield {
      rParam = rParam.randomizeValue(rand)
      rParam.getNaturalValue
    }
    assertResult(expectedRandomValues) { actResult }
  }

  test("equality") {
    val otherParam = createParameter
    if (parameter != otherParam) {
      println("pams not eq p=" + parameter.toString + " other = " + otherParam.toString)
    }
    assertResult(parameter) {otherParam}
  }

  test("inequality") {
    val otherParam = createOtherParameter
    assertResult(false) {parameter == otherParam}
  }

  protected def expectedIsIntegerOnly: Boolean = false
  protected def expectedMinValue: Double
  protected def expectedMaxValue: Double
  protected def expectedRange: Double
  protected def expectedValue: Double
  protected def expectedTweakedValues: Array[T]
  protected def expectedRandomValues: Array[T]
  protected def expectedNaturalValue: Any
  protected def expectedForwardEpsChange: Double
  protected def expectedBackwardEpsChange: Double
}
// Copyright by Barry G. Becker, 2013-2014. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.types

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.Direction
import org.junit.Assert.assertEquals
import org.scalatest.{BeforeAndAfter, FunSuite}
import ParameterSuite.RAND
import scala.util.Random

/**
  * Base class for all parameter test classes
  * @author Barry Becker
  */
object ParameterSuite  {
  private val RAND: Random = new Random(1)
}

abstract class ParameterSuite[T] extends FunSuite with BeforeAndAfter {
  /** instance under test */
  protected var parameter: Parameter = _

  before {
    parameter = createParameter
  }

  protected def createParameter: Parameter

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
    parameter.incrementByEps(Direction.FORWARD)
    assertEquals("Unexpected eps forward", expectedForwardEpsChange, parameter.getValue, MathUtil.EPS_MEDIUM)
  }

  test("IncrementByEpsBackward") {
    parameter.incrementByEps(Direction.BACKWARD)
    assertEquals("Unexpected eps backward", expectedBackwardEpsChange, parameter.getValue, MathUtil.EPS_MEDIUM)
  }

  test("TweakedValues") {
    parameter.tweakValue(0.02, RAND)
    val v1: Any = parameter.getNaturalValue
    parameter.tweakValue(0.1, RAND)
    val v2: Any = parameter.getNaturalValue
    parameter.tweakValue(0.4, RAND)
    val v3: Any = parameter.getNaturalValue
    parameter.tweakValue(0.8, RAND)
    val v4: Any = parameter.getNaturalValue
    parameter.tweakValue(1.1, RAND)
    val v5: Any = parameter.getNaturalValue
    //assertArrayEquals("Unexpected tweaked values", expectedTweakedValues(), new Object[] {v1, v2, v3});
    assertResult(expectedTweakedValues) { Array(v1, v2, v3, v4, v5) }
  }

  protected def expectedIsIntegerOnly: Boolean = false
  protected def expectedMinValue: Double
  protected def expectedMaxValue: Double
  protected def expectedRange: Double
  protected def expectedValue: Double
  protected def expectedTweakedValues: Array[T]
  protected def expectedNaturalValue: Any
  protected def expectedForwardEpsChange: Double
  protected def expectedBackwardEpsChange: Double
}
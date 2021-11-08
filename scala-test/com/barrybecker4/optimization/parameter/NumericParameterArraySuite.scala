package com.barrybecker4.optimization.parameter

import com.barrybecker4.math.MathUtil
import org.scalatest.{BeforeAndAfter}
import NumericParameterArraySuite.createParamArray
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random


/**
  * @author Barry Becker
  */
object NumericParameterArraySuite {
  private val MIN_VALUE = -20.0
  private val MAX_VALUE = 20.0
  private val RAND = new Random(1)

  def createParamArray(value1: Double, value2: Double): NumericParameterArray = {
    assert(value1 >= MIN_VALUE && value1 <= MAX_VALUE)
    assert(value2 >= MIN_VALUE && value2 <= MAX_VALUE)
    new NumericParameterArray(
      Array[Double](value1, value2), // values
      Array[Double](MIN_VALUE, MIN_VALUE), // min
      Array[Double](MAX_VALUE, MAX_VALUE), // max
      Array[String]("A", "B"), // names
      RAND)
  }
}

class NumericParameterArraySuite extends AnyFunSuite with BeforeAndAfter {

  private var params: NumericParameterArray = _

  before {
    MathUtil.RANDOM.setSeed(0)
  }

  test("GetNumSteps") {
    params = createParamArray(.2, .3)
    assertResult(10) { params.numSteps }
  }

  test("GetSamplePopulationSize") {
    params = NumericParameterArraySuite.createParamArray(.2, .3)
    assertResult(144) { params.getSamplePopulationSize }
  }

  test("Find0GlobalSamples") {
    // expect[[IllegalArgumentException]
    params = createParamArray(.2, .3)
    assertThrows[IllegalArgumentException] {
      params.findGlobalSamples(0)
    }
  }

  test("Find1GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(1))
    assertResult(1)  { samples.length }
    val expParams = Array(createParamArray(0, 0.0))
    assertResult(expParams) { samples }
  }

  test("Find2GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(2))
    assertResult(1) { samples.length }
  }

  test("Find3GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(3))
    assertResult( 1) { samples.length }
  }

  test("Find4GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(4))
    assertResult(4) { samples.length }
    val expParams = Array(
      createParamArray(-10.0, -10.0),
      createParamArray(-10.0, 10.0),
      createParamArray(10.0, -10.0),
      createParamArray(10.0, 10.0)
    )
    assertResult(expParams) { samples }
  }

  test("Find10GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(10))
    assertResult(9 ) { samples.length }
  }

  test("Find97GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult( 81) { samples.length }
  }

  test("Find1000GlobalSamples") {
    params = createParamArray(.2, .3)
    val samples = getListFromIterator(params.findGlobalSamples(1000))
    assertResult(961) { samples.length }
  }

  private def getListFromIterator(iter: Iterator[NumericParameterArray]): Array[NumericParameterArray] =
    iter.toArray
}

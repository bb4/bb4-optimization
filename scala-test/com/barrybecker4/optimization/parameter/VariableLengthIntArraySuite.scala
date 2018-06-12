package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.parameter.distancecalculators.{DistanceCalculator, MagnitudeDistanceCalculator, MagnitudeIgnoredDistanceCalculator}
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.{BeforeAndAfter, FunSuite}
import scala.collection.mutable.ArrayBuffer


class VariableLengthIntArraySuite extends FunSuite with BeforeAndAfter{

  private var params: VariableLengthIntArray = _

  before {
    MathUtil.RANDOM.setSeed(0)
  }

  test("GetNumSteps") {
    params = createDistArray(2, -1, 3, -1)
    assertResult(4) { params.getMaxLength }
  }

  test("SimilarityWhenEqual") {
    params = createDistArray(2, -1, 3, -1)
    val otherParams = createDistArray(2, -1, 3, -1)
    assertResult(0.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenEqualSizeButDifferentValues") {
    params = createDistArray(2, -1, 3, -1)
    val otherParams = createDistArray(2, -1, 3, -2)
    assertResult(1.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenEqualSizeButVeryDifferentValues") {
    params = createDistArray(2, -99, 3, -1)
    val otherParams = createDistArray(2, -1, 30, -2)
    assertResult(124.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenUnequalSizes") {
    params = createDistArray(2, -1, 3, -1)
    val otherParams = createDistArray(2, -1, 3, -1, 1)
    assertResult(1.0) { params.distance(otherParams) }
  }

  test("GetSamplePopulationSizeWhenSmall"){
    params = createDistArray(2, -1, 3, -1)
    assertResult(81) { params.getSamplePopulationSize }
  }

  test("GetSamplePopulationSizeWhenLarge") {
    params = createDistArray(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6, -17, 11)
    assertResult(4000) { params.getSamplePopulationSize }
  }

  test("Find0GlobalSamples") {
    params = createDistArray(2, -1, 3, -1)
    params.findGlobalSamples(0)
  }

  test("Find1GlobalSamples") {
    params = createDistArray(2, -1, 3, -1)
    val samples = getListFromIterator(params.findGlobalSamples(1))
    assertResult(1) { samples.length }
    val expParams = Array(createDistArray(2, 3, -1))
    assertResult(expParams) { samples }
  }

  test("Find2GlobalSamples") {
    params = createDistArray(2, -1, 3, -1)
    val samples = getListFromIterator(params.findGlobalSamples(2))
    assertResult(2) { samples.length }
  }

  test("Find3GlobalSamples") {
    params = createDistArray(2, -1, 3, -1)
    val samples = getListFromIterator(params.findGlobalSamples(3))
    assertResult(3) { samples.length }
  }

  test("Find4GlobalSamples(") {
    params = createDistArray(2, -1, 3, -1)
    val samples = getListFromIterator(params.findGlobalSamples(4))
    assertResult( 4) { samples.length }
    val expParams = Array(
      createDistArray(2, 3, -1),
      createDistArray(2, -1),
      createDistArray(3, -1),
      createDistArray(-1, -1)
    )
    assertResult(expParams) { samples }
  }

  test("Find10GlobalSamples") {
    params = createDistArray(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6)
    val samples = getListFromIterator(params.findGlobalSamples(10))
    assertResult(10)  { samples.length }
  }

  test("Find97GlobalSamples") {
    params = createDistArray(2, -1, 3, -1)
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult(15) { samples.length }
  }

  private def getListFromIterator(iter: Iterator[VariableLengthIntArray]): Array[VariableLengthIntArray] = {
    iter.toArray
  }

  private def createArray(dCalc: DistanceCalculator, numberList: Seq[Int]) = {
    val params = ArrayBuffer[Parameter]()
    for (i <- numberList) {
      params.append(createParam(i))
    }
    VariableLengthIntArray.createInstance(params.toArray, numberList, dCalc)
  }

  private def createDistArray(numberList: Int*) =
    createArray(new MagnitudeDistanceCalculator, numberList)

  def createDistIgnoredArray(numberList: Int*): VariableLengthIntArray =
    createArray(new MagnitudeIgnoredDistanceCalculator, numberList)

  private def createParam(i: Int): Parameter = {
    val min = if (i < 0) i else 0
    val max = if (i >= 0) i else 0
    new IntegerParameter(i, min, max, "p" + i)
  }
}

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
    params = createDistArray(Set(2, -1, 3, -1))
    assertResult("f") { params.toString }
    assertResult(3) { params.getMaxLength }
  }

  test("SimilarityWhenEqual") {
    params = createDistArray(Set(2, -1, 3, -1))
    val otherParams = createDistArray(Set(2, -1, 3, -1))
    assertResult(0.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenEqualSizeButDifferentValues") {
    params = createDistArray(Set(2, -1, 3, -1))
    val otherParams = createDistArray(Set(2, -1, 3, -2))
    assertResult(2.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenEqualSizeButVeryDifferentValues") {
    params = createDistArray(Set(2, -99, 3, -1))
    val otherParams = createDistArray(Set(2, -1, 30, -2))
    assertResult(124.0) { params.distance(otherParams) }
  }

  test("SimilarityWhenUnequalSizes") {
    params = createDistArray(Set(2, -1, 3, -1))
    val otherParams = createDistArray(Set(2, -1, 3, -1, 1))
    assertResult(1.0) { params.distance(otherParams) }
  }

  test("GetSamplePopulationSizeWhenSmall"){
    params = createDistArray(Set(2, -1, 3, -1))
    assertResult(27) { params.getSamplePopulationSize }
  }

  test("GetSamplePopulationSizeWhenLarge") {
    params = createDistArray(Set(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6, -17, 11))
    assertResult(4000) { params.getSamplePopulationSize }
  }

  test("Find0GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1))
    params.findGlobalSamples(0)
  }

  test("Find1GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1))
    val samples = getListFromIterator(params.findGlobalSamples(1))
    assertResult(1) { samples.length }
    val expParams = Array(createDistArray(Set(-1, 3, 2)))
    assertResult(expParams) { samples }
  }

  test("Find2GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1))
    val samples = getListFromIterator(params.findGlobalSamples(2))
    assertResult(2) { samples.length }
  }

  test("Find3GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1))
    val samples = getListFromIterator(params.findGlobalSamples(3))
    assertResult(3) { samples.length }
  }

  test("Find4GlobalSamples(") {
    params = createDistArray(Set(2, -1, 3, -1))
    val samples = getListFromIterator(params.findGlobalSamples(4))
    assertResult(4) { samples.length }
    val expParams = Array(
      createDistArray(Set(-1, 3, 2)),
      createDistArray(Set(-1, 2)),
      createDistArray(Set(-1, 3)),
      createDistArray(Set(-1, -1))
    )
    assertResult(expParams) { samples }
  }

  test("Find10GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6))
    val samples = getListFromIterator(params.findGlobalSamples(10))
    assertResult(10)  { samples.length }
  }

  test("Find97GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -1))
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult(15) { samples.length }
  }

  test("swap nodes (4 params). r = 1.2") {
    params = createDistArray(Set(2, -1, 3, -1))
    val nbr = params.getRandomNeighbor(1.2)
    assertResult(
      """
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p3 = 3.0 [0, 3.0]
        |parameter[2] = p-1 = -1.00 [-1.00, 0]
        |fitness = 0.0"""
        .stripMargin.replaceAll("\r\n", "\n")) { nbr.toString }
  }

  test("swap nodes (4 params). r =  0.3") {
    params = createDistArray(Set(2, -1, 3, -1))
    val nbr = params.getRandomNeighbor(0.3)
    assertResult(
      """
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p3 = 3.0 [0, 3.0]
        |parameter[2] = p-1 = -1.00 [-1.00, 0]
        |fitness = 0.0"""
        .stripMargin.replaceAll("\r\n", "\n")) { nbr.toString }
  }

  test("swap nodes (11 params). r = 1.2") {
    params = createDistArray(Set(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6))
    val nbr = params.getRandomNeighbor(1.2)
    assertResult(
      """
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p-1 = -1.00 [-1.00, 0]
        |parameter[2] = p-1 = -1.00 [-1.00, 0]
        |parameter[3] = p3 = 3.0 [0, 3.0]
        |parameter[4] = p-4 = -4.0 [-4.0, 0]
        |parameter[5] = p-2 = -2.0 [-2.0, 0]
        |parameter[6] = p-3 = -3.0 [-3.0, 0]
        |parameter[7] = p5 = 5.0 [0, 5.0]
        |parameter[8] = p-9 = -9.0 [-9.0, 0]
        |parameter[9] = p6 = 6.0 [0, 6.0]
        |fitness = 0.0"""
        .stripMargin.replaceAll("\r\n", "\n")) { nbr.toString }
  }

  test("swap nodes (11 params). r =  0.3") {
    params = createDistArray(Set(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6))
    val nbr = params.getRandomNeighbor(0.3)
    assertResult(
      """
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p-1 = -1.00 [-1.00, 0]
        |parameter[2] = p-1 = -1.00 [-1.00, 0]
        |parameter[3] = p3 = 3.0 [0, 3.0]
        |parameter[4] = p-4 = -4.0 [-4.0, 0]
        |parameter[5] = p-2 = -2.0 [-2.0, 0]
        |parameter[6] = p-3 = -3.0 [-3.0, 0]
        |parameter[7] = p5 = 5.0 [0, 5.0]
        |parameter[8] = p-9 = -9.0 [-9.0, 0]
        |parameter[9] = p6 = 6.0 [0, 6.0]
        |fitness = 0.0"""
        .stripMargin.replaceAll("\r\n", "\n")) { nbr.toString }
  }
  private def getListFromIterator(iter: Iterator[VariableLengthIntArray]): Array[VariableLengthIntArray] = {
    iter.toArray
  }

  private def createArray(dCalc: DistanceCalculator, numberList: Set[Int]) = {
    val params = for (i <- numberList) yield createParam(i)
    VariableLengthIntArray.createInstance(params.toArray, numberList, dCalc)
  }

  private def createDistArray(numberList: Set[Int]) =
    createArray(new MagnitudeDistanceCalculator, numberList)

  def createDistIgnoredArray(numberList: Set[Int]): VariableLengthIntArray =
    createArray(new MagnitudeIgnoredDistanceCalculator, numberList)

  private def createParam(i: Int): Parameter = {
    val min = if (i < 0) i else 0
    val max = if (i >= 0) i else 0
    new IntegerParameter(i, min, max, "p" + i)
  }
}

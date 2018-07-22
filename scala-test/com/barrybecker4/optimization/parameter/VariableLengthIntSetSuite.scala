package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.distancecalculators.{DistanceCalculator, MagnitudeIgnoredDistanceCalculator}
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.{BeforeAndAfter, FunSuite}
import com.barrybecker4.common.testsupport.strip
import scala.util.Random


class VariableLengthIntSetSuite extends FunSuite with BeforeAndAfter{

  private var params: VariableLengthIntSet = _
  private var rnd: Random = _
  before {
    rnd = new Random(1)
  }

  test("serialization of typical") {
    params = createDistArray(Set(2, -1, 3, -4))
    assertResult(strip("""
            |parameter[0] = p2 = 2.0 [0, 2.0]
            |parameter[1] = p-1 = -1.00 [-1.00, 0]
            |parameter[2] = p3 = 3.0 [0, 3.0]
            |parameter[3] = p-4 = -4.0 [-4.0, 0]
            |""")) { params.toString }
    assertResult(4) { params.getMaxLength }
  }

  test("serialization when duplicates present") {
    params = createDistArray(Set(2, -1, 3, -1))
    assertResult(strip("""
           |parameter[0] = p2 = 2.0 [0, 2.0]
           |parameter[1] = p-1 = -1.00 [-1.00, 0]
           |parameter[2] = p3 = 3.0 [0, 3.0]
           |""")) { params.toString }
    assertResult(3) { params.getMaxLength }
  }

  test("serialization when several duplicates present") {
    params = createDistArray(Set(2, -1, 2, 3, 2, -1))
    assertResult(strip("""
           |parameter[0] = p2 = 2.0 [0, 2.0]
           |parameter[1] = p-1 = -1.00 [-1.00, 0]
           |parameter[2] = p3 = 3.0 [0, 3.0]
           |""")) { params.toString }
    assertResult(3) { params.getMaxLength }
  }

  test("Similarity when identically equal") {
    params = createDistArray(Set(2, -1, 3, -4))
    assertResult(0.0) { params.distance(params) }
    assertResult(params) {params}
  }

  test("Similarity when equal") {
    params = createDistArray(Set(2, -1, 3, -4))
    val otherParams = createDistArray(Set(2, -1, 3, -4))
    assertResult(0.0) { params.distance(otherParams) }
    assertResult(params) {otherParams}
  }

  test("Similarity when equal, but values in different order") {
    params = createDistArray(Set(2, -1, 3, -4))
    val otherParams = createDistArray(Set(2, 3, -1, -4))
    assertResult(0.0) { params.distance(otherParams) }
    assertResult(params) {otherParams}
  }

  test("Similarity when equal size but different values") {
    params = createDistArray(Set(2, -1, 3, -4))
    val otherParams = createDistArray(Set(2, -1, 3, -2))
    assertResult(2.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("SimilarityWhenEqualSizeButVeryDifferentValues") {
    params = createDistArray(Set(2, -99, 3, -1))
    val otherParams = createDistArray(Set(2, -1, 30, -2))
    assertResult(124.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("SimilarityWhenUnequalSizes") {
    params = createDistArray(Set(2, -1, 3, -4))
    val otherParams = createDistArray(Set(2, -1, 3, -2, 1))
    assertResult(3.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("getCombination") {
    params = createDistArray(Set(2, -1, 3, -4))
    assertResult(createDistArray(Set(3, -4))) { createDistArray(Set(-4, 3))}
  }

  test("GetSamplePopulationSizeWhenSmall"){
    params = createDistArray(Set(2, -1, 3, -4))
    assertResult(256) { params.getSamplePopulationSize }
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
    params = createDistArray(Set(2, -1, 3))
    val samples = getListFromIterator(params.findGlobalSamples(1))
    assertResult(1) { samples.length }
    val expParams = Array(
      createDistArray(Seq(-1, 3), Set(2, -1, 3))
    )
    assertResult(expParams) { samples }
  }

  test("Find2GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(2))
    assertResult(2) { samples.length }
  }

  test("Find3GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(3))
    assertResult(3) { samples.length }
  }

  /*
  test("Find4GlobalSamples") {
    params = createDistArray(Set(2, -1, 3))
    val samples = getListFromIterator(params.findGlobalSamples(4))
    assertResult(4) { samples.length }
    val expParams = Array(
      createDistArray(Seq(-1, 3), Set(2, -1, 3)),
      createDistArray(Seq(3, 2), Set(2, -1, 3)),
      createDistArray(Seq(-1, 2), Set(2, -1, 3)),
      createDistArray(Seq(2, -1), Set(2, -1, 3))
    )
    assertResult(expParams) { samples }
  }*/

  test("Find10GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val samples = getListFromIterator(params.findGlobalSamples(10))
    assertResult(10)  { samples.length }
  }

  test("Find97GlobalSamples") {
    params = createDistArray(Set(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult(15) { samples.length }
  }

  test("swap nodes (4 params). r = 1.2") {
    params = createDistArray(Set(2, -1, 3, -4))
    val nbr = params.getRandomNeighbor(1.2)
    assertResult(strip("""
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p3 = 3.0 [0, 3.0]
        |parameter[2] = p-4 = -4.0 [-4.0, 0]
        |""")) { nbr.toString }

    val nbr2 = params.getRandomNeighbor(1.2)
    assertResult(strip("""
       |parameter[0] = p-1 = -1.00 [-1.00, 0]
       |parameter[1] = p3 = 3.0 [0, 3.0]
       |parameter[2] = p-4 = -4.0 [-4.0, 0]
       |""")) { nbr2.toString }
  }

  test("swap nodes (4 params). r =  0.3") {
    params = createDistArray(Set(2, -1, 3, -4))
    val nbr = params.getRandomNeighbor(0.3)
    assertResult(strip("""
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p3 = 3.0 [0, 3.0]
        |parameter[2] = p-4 = -4.0 [-4.0, 0]
        |""")) { nbr.toString }
  }

  test("swap nodes (11 params). r = 1.2") {
    params = createDistArray(Set(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val nbr = params.getRandomNeighbor(1.2)
    assertResult(strip("""
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p-5 = -5.0 [-5.0, 0]
        |parameter[2] = p-3 = -3.0 [-3.0, 0]
        |parameter[3] = p-4 = -4.0 [-4.0, 0]
        |parameter[4] = p3 = 3.0 [0, 3.0]
        |parameter[5] = p-2 = -2.0 [-2.0, 0]
        |parameter[6] = p-1 = -1.00 [-1.00, 0]
        |parameter[7] = p5 = 5.0 [0, 5.0]
        |parameter[8] = p-9 = -9.0 [-9.0, 0]
        |""")) { nbr.toString }
  }

  test("swap nodes (11 params). r =  0.3") {
    params = createDistArray(Set(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val nbr = params.getRandomNeighbor(0.3)
    assertResult(strip("""
        |parameter[0] = p2 = 2.0 [0, 2.0]
        |parameter[1] = p-5 = -5.0 [-5.0, 0]
        |parameter[2] = p-3 = -3.0 [-3.0, 0]
        |parameter[3] = p-4 = -4.0 [-4.0, 0]
        |parameter[4] = p3 = 3.0 [0, 3.0]
        |parameter[5] = p-2 = -2.0 [-2.0, 0]
        |parameter[6] = p-1 = -1.00 [-1.00, 0]
        |parameter[7] = p5 = 5.0 [0, 5.0]
        |parameter[8] = p-9 = -9.0 [-9.0, 0]
        |""")) { nbr.toString }
  }
  private def getListFromIterator(iter: Iterator[VariableLengthIntSet]): Array[VariableLengthIntSet] =
    iter.toArray

  private def createArray(dCalc: DistanceCalculator, numberList: Set[Int]) = {
    val params = for (i <- numberList) yield createParam(i)
    new VariableLengthIntSet(params.toIndexedSeq, numberList, dCalc, rnd)
  }

  private def createDistArray(numberList: Set[Int]) = {
    val params = for (i <- numberList) yield createParam(i)
    VariableLengthIntSet.createInstance(params.toIndexedSeq, numberList, rnd)
  }

  private def createDistArray(numbers: Seq[Int], fullList: Set[Int]) = {
    val params = for (i <- numbers) yield createParam(i)
    VariableLengthIntSet.createInstance(params.toIndexedSeq, fullList, rnd)
  }

  def createDistIgnoredArray(numberList: Set[Int]): VariableLengthIntSet =
    createArray(new MagnitudeIgnoredDistanceCalculator, numberList)

  private def createParam(i: Int): Parameter = {
    val min = if (i < 0) i else 0
    val max = if (i >= 0) i else 0
    new IntegerParameter(i, min, max, "p" + i)
  }
}

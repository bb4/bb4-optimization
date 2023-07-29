package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.distancecalculators.{DistanceCalculator, MagnitudeIgnoredDistanceCalculator}
import org.scalatest.BeforeAndAfter
import com.barrybecker4.common.testsupport.strip

import scala.util.Random
import com.barrybecker4.optimization.parameter.VariableLengthIntSet.createParam
import org.scalatest.funsuite.AnyFunSuite


class VariableLengthIntSetSuite extends AnyFunSuite with BeforeAndAfter {

  private var params: VariableLengthIntSet = _
  private var rnd: Random = _
  before {
    rnd = new Random(1)
  }

  test("serialization of typical") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    assertResult(strip("""
            |parameter[0] = p2 = 2.0 [0, 2.0]
            |parameter[1] = p-1 = -1.00 [-1.00, 0]
            |parameter[2] = p3 = 3.0 [0, 3.0]
            |parameter[3] = p-4 = -4.0 [-4.0, 0]
            |""")) { params.toString }
    assertResult(4) { params.getMaxLength }
  }

  test("serialization when duplicates present") {
    params = createDistArray(IndexedSeq(2, -1, 3, -1))
    assertResult(strip("""
           |parameter[0] = p2 = 2.0 [0, 2.0]
           |parameter[1] = p-1 = -1.00 [-1.00, 0]
           |parameter[2] = p3 = 3.0 [0, 3.0]
           |""")) { params.toString }
    assertResult(3) { params.getMaxLength }
  }

  test("serialization when several duplicates present") {
    params = createDistArray(IndexedSeq(2, -1, 2, 3, 2, -1))
    assertResult(strip("""
           |parameter[0] = p2 = 2.0 [0, 2.0]
           |parameter[1] = p-1 = -1.00 [-1.00, 0]
           |parameter[2] = p3 = 3.0 [0, 3.0]
           |""")) { params.toString }
    assertResult(3) { params.getMaxLength }
  }

  test("Similarity when identically equal") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    assertResult(0.0) { params.distance(params) }
    assertResult(params) {params}
  }

  test("Similarity when equal") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val otherParams = createDistArray(IndexedSeq(2, -1, 3, -4))
    assertResult(0.0) { params.distance(otherParams) }
    assertResult(params) {otherParams}
  }

  test("Similarity when equal, but values in different order") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val otherParams = createDistArray(IndexedSeq(2, 3, -1, -4))
    assertResult(0.0) { params.distance(otherParams) }
    assertResult(params) {otherParams}
  }

  test("Similarity when equal size but different values") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val otherParams = createDistArray(IndexedSeq(2, -1, 3, -2))
    assertResult(2.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("Similarity when equal size but very different values") {
    params = createDistArray(IndexedSeq(2, -99, 3, -1))
    val otherParams = createDistArray(IndexedSeq(2, -1, 30, -2))
    assertResult(124.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("Similarity when unequal sizes") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val otherParams = createDistArray(IndexedSeq(2, -1, 3, -2, 1))
    assertResult(3.0) { params.distance(otherParams) }
    assert(params != otherParams)
  }

  test("getCombination with values in order") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val combo = params.getCombination(IndexedSeq(2, 3))
    val expected = createIntArray(IndexedSeq(3, -4), IndexedSeq(2, -1, 3, -4))
    assertResult(expected.toString) {combo.toString}
    assertResult(expected) { combo }
  }

  test("getCombination with values out of order") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val combo = params.getCombination(IndexedSeq(3, 2))
    val expected = createIntArray(IndexedSeq(3, -4), IndexedSeq(2, -1, 3, -4))
    assertResult(expected) { combo }
  }

  test("getCombination of all values") {
    val combo = params.getCombination(IndexedSeq(1, 0, 3, 2))
    val expected = createIntArray(IndexedSeq(2, -1, 3, -4), IndexedSeq(2, -1, 3, -4))
    assertResult(expected) { combo }
  }

  test("GetSamplePopulationSizeWhenSmall"){
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    assertResult(256) { params.getSamplePopulationSize }
  }

  test("GetSamplePopulationSizeWhenLarge") {
    params = createDistArray(IndexedSeq(2, -1, 3, -1, 3, -4, -2, -3, 5, -9, 6, -17, 11))
    assertResult(4000) { params.getSamplePopulationSize }
  }

  test("Find 0 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3, -1))
    assertThrows[NoSuchElementException] {
      getListFromIterator(params.findGlobalSamples(0))
    }
  }

  test("Find 1 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3))
    val samples = getListFromIterator(params.findGlobalSamples(1))
    assertResult(1) { samples.length }
    val expParams = IndexedSeq(
      createDistArray(Seq(-1), IndexedSeq(2, -1, 3))
    )
    assertResult(expParams) { samples }
  }

  test("Find 2 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(2))
    assertResult(2) { samples.length }
  }

  test("Find 3 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(3))
    assertResult(3) { samples.length }
  }

  /* This may a problem because there are not 4 global samples of 3 values.
     The only combinations are 2 -1, -1 3, 2 3. */
  test("Find 4 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3))
    val samples = getListFromIterator(params.findGlobalSamples(4))
    assertResult(4) { samples.length }
    val expParams = IndexedSeq(
      createDistArray(Seq(2), IndexedSeq(2, -1, 3)),
      createDistArray(Seq(-1), IndexedSeq(2, -1, 3)),
      createDistArray(Seq(2, -1), IndexedSeq(2, -1, 3)),
      createDistArray(Seq(3), IndexedSeq(2, -1, 3))
    )
    assertResult(expParams) { samples }
  }

  /* This may a problem because there are not 4 global samples of 3 values.
     The only combinations are 2 -1, -1 3, 2 3. */
  test("Find 9 GlobalSamples when only 7") {
    params = createDistArray(IndexedSeq(2, -1, 3))
    val samples = getListFromIterator(params.findGlobalSamples(9))
    assertResult(7) { samples.length }
  }

  test("Find 10 GlobalSamples") {
    params = createDistArray(IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val samples = getListFromIterator(params.findGlobalSamples(10))
    assertResult(10)  { samples.length }
  }

  test("Find 15 GlobalSamples when only 15") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(15))
    assertResult(15) { samples.length }
  }

  test("Find 97 GlobalSamples from array of 6 elements") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4, -7, 9, 7))
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult(97) { samples.length }
  }

  test("Find 97 GlobalSamples when only 15") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val samples = getListFromIterator(params.findGlobalSamples(97))
    assertResult(15) { samples.length }
  }

  test("random neighbor (4 params). r = 1.6") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val radius = 1.6
    val nbrs = for (i <- 1 to 20) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3
        |-1, 3, -4
        |2, -1, -4
        |-1, 3, -4
        |2, -1, -4
        |2, -4, 3
        |2, -1, 3
        |2, 3, -4
        |2, -1, 3
        |-1, 3, -4
        |2, -1, 3
        |2, -1, 3
        |2, 3
        |2, -1, 3
        |2, 3
        |-1, 3, -4
        |2, -1, -4
        |-4, 3
        |2, 3, -4
        |-1, 3, -4""".stripMargin)) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (4 params). r = 1.2") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val radius = 1.2
    val nbrs = for (i <- 1 to 20) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3
        |-1, 3, -4
        |2, -1, -4
        |-1, 3, -4
        |2, -1, -4
        |2, -4, 3
        |2, -1, 3
        |2, 3, -4
        |2, -1, 3
        |-1, 3, -4
        |2, -1, 3
        |2, -1, 3
        |2, 3, -4
        |2, -1, 3
        |2, 3
        |-1, 3, -4
        |2, -1, -4
        |-4, 3
        |2, -1, -4
        |-1, 3, -4""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (4 params). r =  0.3") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val radius = 0.3
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3
        |-1, 3, -4
        |2, -1, -4
        |-1, 3, -4
        |2, -1, -4
        |-1, 3, -4
        |2, -1, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (4 params). r =  0.1") {
    params = createDistArray(IndexedSeq(2, -1, 3, -4))
    val radius = 0.1
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3
        |2, -1, 3
        |-1, 3, -4
        |2, -1, -4
        |2, -1, -4
        |2, -1, 3
        |2, -1, -4""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (all 11 params). r = 1.2") {
    params = createDistArray(IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 1.2
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3, -5, -4, -3, 5, -9, 6
        |2, -1, 3, -5, -4, -2, -3, 5, 6
        |2, -5, -2, -3, 6
        |2, -1, 3, -5, -4, -2, 5, -9, 6
        |2, -4
        |2, -1, 3, -5, -2, -3, -4, -9, 6
        |2, -1, 3, -5, -4, -2, -3, 5, 6""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (11 params). r =  0.3") {
    params = createDistArray(IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 0.3
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """2, -1, 3, -5, -4, -3, 5, -9, 6
        |2, -1, 3, -5, -4, -2, -3, 5, 6
        |2, -1, -5, -4, -2, -3, 5, -9, 6
        |2, -1, 3, -5, -4, -2, 5, -9, 6
        |2, -1, 3, -4, -2, -3, 5, -9, 6
        |2, -1, 3, -5, -2, -3, -9, 6
        |2, -1, 3, -5, -4, -2, 5, -9, 6""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (5 of 11 params). r = 1.2") {
    params = createDistArray(Seq(-4, -9, 2, 6, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 1.2
    val nbrs = for (i <- 1 to 10) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """-4, -9, -5, 6, 3
        |-9, 6, 2
        |-4, 6, -3
        |-3, -9, 2, 6, 3, -1, -5, -2
        |-4, -9, 2, 3
        |-9, 2, 6, 3
        |-4, -9, -1, 6, 3
        |2, 3
        |-4, 5, 2, -1, 3
        |-3, -9, 2, 6, 3, -2""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (5 of 11 params). r =  0.3") {
    params = createDistArray(Seq(-4, -9, 2, 6, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 0.3
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """-4, -9, -5, 6, 3
        |-9, 2, 6, 3
        |-4, -9, 2, 6, 3, -2
        |-4, -9, -3, 6, 3
        |-4, -9, -1, 6, 3
        |-4, 2, 6, 3
        |-4, -9, 2, -3, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (5 of 11 params). r =  0.1") {
    params = createDistArray(Seq(-4, -9, 2, 6, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 0.1
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """-4, -9, -5, 6, 3
        |-9, 2, 6, 3
        |-4, -9, 2, 6, 3, -2
        |-4, -9, -3, 6, 3
        |-4, -9, -1, 6, 3
        |-4, -9, 2, -2, 3
        |-4, -2, 2, 6, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (5 of 11 params). r =  0.01") {
    params = createDistArray(Seq(-4, -9, 2, 6, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6))
    val radius = 0.01
    val nbrs = for (i <- 1 to 7) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """-4, -9, -5, 6, 3
        |5, -9, 2, 6, 3
        |-4, 5, 2, 6, 3
        |-4, -9, 2, -5, 3
        |-4, -9, 2, 6, 3, -2
        |-4, -9, -3, 6, 3
        |-4, -9, -2, 6, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (3 of 12 params). r = 1.2") {
    params = createDistArray(Seq(-9, 2, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6, -7))
    val radius = 1.2
    val nbrs = for (i <- 1 to 10) yield { params.getRandomNeighbor(radius).intValues }

    assertResult(strip(
      """-9, -5, 3
        |-3, 2, -1
        |-9, 2, 3, -5, -7, -2, -3, -1
        |-9, 2, 3, 6, -4, -7, -2, -5, 5, -3, -1
        |-9, 2, 3, -7, -2, -5, -1, -4, 5, 6
        |-9, -4, 3, 5
        |-9, 5
        |2, 3
        |-2, 2, 3, -1, -3
        |-9, 2, 3, -1, -4, -5, -3, -2, 6, 5, -7""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (3 of 12 params). r =  0.3") {
    params = createDistArray(Seq(-9, 2, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6, -7))
    val radius = 0.3
    val nbrs = for (i <- 1 to 10) yield {
      params.getRandomNeighbor(radius).intValues
    }

    assertResult(strip(
      """-9, -5, 3
        |-3, 2, 3
        |-9, 2, 3, -5
        |-9, 6, 3
        |-5, 2, 3
        |2, 3
        |2, 3
        |-9, 2, 3, -1
        |-9, 2, -1
        |6, 2, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (3 of 12 params). r =  0.1") {
    params = createDistArray(Seq(-9, 2, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6, -7))
    val radius = 0.1
    val nbrs = for (i <- 1 to 20) yield {
      params.getRandomNeighbor(radius).intValues
    }

    assertResult(strip(
      """-9, -5, 3
        |-3, 2, 3
        |-9, 6, 3
        |-7, 2, 3
        |-9, -5, 3
        |-9, -3, 3
        |-9, 5, 3
        |-5, 2, 3
        |-9, -1, 3
        |-5, 2, 3
        |-2, 2, 3
        |-1, 2, 3
        |-9, 2, -7
        |-9, 2, 3, -3
        |-9, -2, 3
        |-9, -2, 3
        |-9, 2
        |-9, 2, -2
        |-5, 2, 3
        |-9, 2, 6""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  test("random neighbor (3 of 12 params). r =  0.01") {
    params = createDistArray(Seq(-9, 2, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6, -7))
    val radius = 0.01
    val nbrs = for (i <- 1 to 10) yield {
      params.getRandomNeighbor(radius).intValues
    }

    assertResult(strip(
      """-9, -5, 3
        |-3, 2, 3
        |-9, 6, 3
        |-7, 2, 3
        |-9, -5, 3
        |-9, -3, 3
        |-9, 5, 3
        |-5, 2, 3
        |-9, -1, 3
        |-5, 2, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }


  test("random neighbor (3 of 12 params). r =  0.0001") {
    params = createDistArray(Seq(-9, 2, 3), IndexedSeq(2, -1, 3, -5, 3, -4, -2, -3, 5, -9, 6, -7))
    val radius = 0.0001
    val nbrs = for (i <- 1 to 10) yield {
      params.getRandomNeighbor(radius).intValues
    }

    assertResult(strip(
      """-9, -5, 3
        |-3, 2, 3
        |-9, 6, 3
        |-7, 2, 3
        |-9, -5, 3
        |-9, -3, 3
        |-9, 5, 3
        |-5, 2, 3
        |-9, -1, 3
        |-5, 2, 3""")) {
      nbrs.map(_.mkString(", ")).mkString("\n")
    }
  }

  private def getListFromIterator(iter: Iterator[VariableLengthIntSet]): Array[VariableLengthIntSet] =
    iter.toArray

  private def createArray(dCalc: DistanceCalculator, numberList: IndexedSeq[Int]) = {
    val params = for (i <- numberList) yield createParam(i)
    new VariableLengthIntSet(params, numberList, dCalc, rnd)
  }

  def createIntArray(intParams: IndexedSeq[Int], fullSeq: IndexedSeq[Int]): VariableLengthIntSet = {
    val params = for (num <- intParams) yield createParam(num)
    VariableLengthIntSet.createInstance(params, fullSeq, rnd)
  }

  private def createDistArray(numberList: IndexedSeq[Int]) = {
    var numSet = Set[Int]()
    val params = for (num <- numberList if !numSet.contains(num)) yield {
      numSet = numSet + num
      createParam(num)
    }
    VariableLengthIntSet.createInstance(params, numberList, rnd)
  }

  private def createDistArray(numbers: Seq[Int], fullList: IndexedSeq[Int]) = {
    assert(numbers.size == numbers.toSet.size, "There must not be duplicates")
    val params = for (i <- numbers) yield createParam(i)
    VariableLengthIntSet.createInstance(params.toIndexedSeq, fullList, rnd)
  }

  def createDistIgnoredArray(numberList: IndexedSeq[Int]): VariableLengthIntSet =
    createArray(new MagnitudeIgnoredDistanceCalculator, numberList)
}

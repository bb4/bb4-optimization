package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalactic.{Equality, TolerantNumerics}

import scala.util.Random
import com.barrybecker4.optimization.parameter.PermutedParameterArraySuite.RND
import org.scalatest.funsuite.AnyFunSuite

/**
  * @author Barry Becker
  */
object PermutedParameterArraySuite {
  private val TOL = 0.000001
  private val RND = new Random(1)
}

class PermutedParameterArraySuite extends AnyFunSuite {

  implicit val doubleEq: Equality[Double] =
    TolerantNumerics.tolerantDoubleEquality(PermutedParameterArraySuite.TOL)

  test("the sample population size grows with the number of parameters up to a cap") {
    assertResult(4 * 4 * 4 * 4 * 4)(createPermParameterArray(Array[Int](0, 1, 2, 3, 4), RND).getSamplePopulationSize)
    assertResult(4000)(createPermParameterArray((0 until 7).toArray, RND).getSamplePopulationSize)
  }

  test("the sample population size does not overflow for many parameters") {
    for (size <- Seq(15, 16, 17, 32, 33, 50, 200))
      assertResult(4000, s"for $size parameters")(createPermParameterArray((0 until size).toArray, RND).getSamplePopulationSize)
  }

  test("PermutedNeighbor") {
    val params = createPermParameterArray(Array[Int](0, 1, 2, 3, 4), RND)
    val nbrParams = params.getRandomNeighbor(1.0)
    val expNbr = createPermParameterArray(Array[Int](3, 1, 2, 0, 4), RND)
    assertResult(expNbr) { nbrParams }
  }

  /** radius 5.0 yields two transpositions; second swap applies to the permutation after the first. */
  test("PermutedNeighbor multiple transpositions compose on current state") {
    val rnd = new Random(1)
    val params = createPermParameterArray(Array[Int](0, 1, 2, 3, 4), rnd)
    val nbrParams = params.getRandomNeighbor(5.0)
    val expNbr = createPermParameterArray(Array[Int](3, 1, 0, 2, 4), rnd)
    assertResult(expNbr) { nbrParams }
  }

  private def valuesOf(pa: PermutedParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  test("a neighbor is still a permutation, and the original is not changed") {
    val original = createPermParameterArray((0 until 20).toArray, new Random(7))
    for (radius <- Seq(0.5, 1.0, 3.0, 50.0, 100000.0)) {
      val nbr = original.getRandomNeighbor(radius)
      assertResult((0 until 20))(valuesOf(nbr).sorted)
      assertResult((0 until 20))(valuesOf(original))
    }
  }

  test("a neighbor keeps the name and range of the parameter at each position") {
    val original = createPermParameterArray((0 until 10).toArray, new Random(3))
    val nbr = original.getRandomNeighbor(4.0)
    for (i <- 0 until 10) {
      assertResult("param" + i)(nbr.get(i).name)
      assertResult(9.0)(nbr.get(i).maxValue)
    }
  }

  test("a neighbor at a small radius changes only a few positions") {
    val original = createPermParameterArray((0 until 100).toArray, new Random(5))
    val changed = (0 until 100).count(i => original.getRandomNeighbor(1.0).get(i).getValue != i)
    assert(changed < 100)
    val nbrValues = valuesOf(original.getRandomNeighbor(1.0))
    assert((0 until 100).count(i => nbrValues(i) != i) <= 20, "radius 1 on 100 params is 10 swaps")
  }

  test("an enormous radius gives a well mixed permutation without doing millions of swaps") {
    val original = createPermParameterArray((0 until 50).toArray, new Random(11))
    val start = System.nanoTime()
    val nbr = (1 to 200).map(_ => original.getRandomNeighbor(20000.0)).last
    assert((System.nanoTime() - start) / 1e9 < 2.0, "should take milliseconds, not seconds")
    val unmoved = (0 until 50).count(i => nbr.get(i).getValue == i)
    assert(unmoved <= 8, s"$unmoved of 50 are still in place")
  }

  test("a radius so large that the swap count would overflow an Int still gives a neighbor at once") {
    val original = createPermParameterArray((0 until 30).toArray, new Random(17))
    val start = System.nanoTime()
    for (radius <- Seq(1e8, 1e10, 1e15, 1e300, Double.MaxValue, Double.PositiveInfinity)) {
      assertResult((0 until 30), s"radius $radius")(valuesOf(original.getRandomNeighbor(radius)).sorted)
    }
    assert((System.nanoTime() - start) / 1e9 < 2.0, "should take milliseconds, not seconds")
  }

  test("a radius that gives more swaps than are useful behaves like a shuffle: every position can change") {
    val original = createPermParameterArray((0 until 6).toArray, new Random(13))
    val seen = (1 to 300).flatMap(_ => valuesOf(original.getRandomNeighbor(1000.0)).zipWithIndex.collect { case (v, i) if v != i => i })
    assertResult((0 until 6).toSet)(seen.toSet)
  }

  /** The two param arrays are not the same, but they are equidistant if they differ only
    * in the cyclic offset.
    */
  test("PermutedDistanceEqual") {
    val params1 = createPermParameterArray(Array[Int](2, 1, 0, 3, 4), RND)
    val params2 = createPermParameterArray(Array[Int](1, 0, 3, 4, 2), RND)
    assert(0.0 === params1.distance(params2))
  }

  /** One run of length 2. */
  test("PermutedDistanceAlmostEqual") {
    val params1 = createPermParameterArray(Array[Int](2, 1, 0, 3, 4), RND)
    val params2 = createPermParameterArray(Array[Int](0, 1, 2, 3, 4), RND)
    assert(6.0 === params1.distance(params2))
  }

  /** One run of length 3. */
  test("PermutedDistanceRunLength3") {
    val params1 = createPermParameterArray(Array[Int](3, 1, 0, 2, 4), RND)
    val params2 = createPermParameterArray(Array[Int](4, 1, 3, 0, 2), RND)
    assert(6.0 === params1.distance(params2))
  }

  /** As different as they can be. No runs even when reversed. */
  test("PermutedDistanceMaximumDifferent") {
    val params1 = createPermParameterArray(Array[Int](4, 2, 0, 3, 1))
    val params2 = createPermParameterArray(Array[Int](0, 1, 2, 3, 4))
    assert(32.0 === params1.distance(params2))
  }

  test("ReversePermuted") {
    val params1 = createPermParameterArray(Array[Int](4, 2, 0, 3, 1))
    assertResult("""
     |parameter[0] = param4 = 1.00 [0, 4.0]
     |parameter[1] = param3 = 3.0 [0, 4.0]
     |parameter[2] = param2 = 0 [0, 4.0]
     |parameter[3] = param1 = 2.0 [0, 4.0]
     |parameter[4] = param0 = 4.0 [0, 4.0]
     |"""
      .stripMargin.replace("\r\n", "\n")) { params1.reverse.toString }
  }

  /** Two runs of length 2 */
  test("PermutedDistance2RunsOfLength2") {
    val params1 = createPermParameterArray(Array[Int](4, 2, 0, 5, 3, 1))
    val params2 = createPermParameterArray(Array[Int](1, 5, 3, 0, 4, 2))
    // 2^6 / (2 + 2) -2
    assert( 14.0 === params1.distance(params2))
  }

  private def createPermParameterArray(values: Array[Int], rnd: Random = new Random(1)) = {
    val params: IndexedSeq[Parameter] = for (i <- values.indices) yield {
        new IntegerParameter(values(i), 0, values.length - 1, "param" + i)
      }
    PermutedParameterArray(params, rnd)
  }
}

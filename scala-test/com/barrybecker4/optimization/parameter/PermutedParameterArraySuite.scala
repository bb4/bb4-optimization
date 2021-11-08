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

  test("PermutedNeighbor") {
    val params = createPermParameterArray(Array[Int](0, 1, 2, 3, 4), RND)
    val nbrParams = params.getRandomNeighbor(1.0)
    val expNbr = createPermParameterArray(Array[Int](3, 1, 2, 0, 4), RND)
    assertResult(expNbr) { nbrParams }
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

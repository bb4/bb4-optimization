package com.barrybecker4.optimization.parameter.distancecalculators

import com.barrybecker4.optimization.parameter.PermutedParameterArray
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.FunSuite

import scala.util.Random


class PermutedDistanceCalculatorSuite extends FunSuite {

  val calc = new PermutedDistanceCalculator()

  // Distance is 0 because permutations are the same after shifting one and reversing.
  test("find distance between rotated/reversed permutations") {
    val params1: Array[Parameter] = Array(1, 2, 3, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 4, 3, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(0.0) { calc.findDistance(p1, p2) }
  }

  test("find distance between identical permutations") {
    val params1: Array[Parameter] = Array(1, 3, 2, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 3, 2, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(0.0) { calc.findDistance(p1, p2) }
  }

  test("find distance between similar permutations (4)") {
    val params1: Array[Parameter] = Array(1, 3, 2, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 3, 4, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(6.0) { calc.findDistance(p1, p2) }
  }
  test("find distance between different permutations (4)") {
    val params1: Array[Parameter] = Array(3, 4, 1, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(4, 2, 1, 3).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(6.0) { calc.findDistance(p1, p2) }
  }

  test("find distance between similar permutations (5)") {
    val params1: Array[Parameter] = Array(1, 2, 3, 5, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 2, 5, 3, 4).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(3.333333333333333) { calc.findDistance(p1, p2) }
  }

  test("find distance between different permutations (5)") {
    val params1: Array[Parameter] = Array(3, 5, 2, 4, 1).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 2, 3, 4, 5).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(32.0) { calc.findDistance(p1, p2) }
  }

  test("find distance between similar permutations (7)") {
    val params1: Array[Parameter] = Array(1, 2, 3, 5, 4, 6, 7).map(x => new IntegerParameter(x, 0, 10, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 2, 5, 3, 4, 6, 7).map(x => new IntegerParameter(x, 0, 10, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(5.111111111111111) { calc.findDistance(p1, p2) }
  }

  test("find distance between different permutations (7)") {
    val params1: Array[Parameter] = Array(3, 6, 5, 2, 4, 7, 1).map(x => new IntegerParameter(x, 0, 10, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 2, 3, 4, 7, 6, 5).map(x => new IntegerParameter(x, 0, 10, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(30.0) { calc.findDistance(p1, p2) }
  }

  test("find distance with reversed permutation (should be 0)") {
    val params1: Array[Parameter] = Array(3, 1, 4, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(2, 4, 1, 3).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertResult(0.0) { calc.findDistance(p1, p2) }
  }

  test("find distance between different sizes") {
    val params1: Array[Parameter] = Array(3, 1, 4, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p1 = new PermutedParameterArray(params1)
    val params2: Array[Parameter] = Array(1, 3, 2).map(x => new IntegerParameter(x, 0, 5, "p" + x))
    val p2 = new PermutedParameterArray(params2)

    assertThrows[AssertionError] {
      calc.findDistance(p1, p2)
    }
  }
}

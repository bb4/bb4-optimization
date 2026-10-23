// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class OrderCrossoverSuite extends AnyFunSuite {

  private def permutation(values: Seq[Int], rnd: Random = new Random(1)): PermutedParameterArray =
    PermutedParameterArray(values.indices.map(i => IntegerParameter(values(i), 0, 1000, "p" + i): Parameter), rnd)

  private def valuesOf(pa: ParameterArray): Seq[Double] = (0 until pa.size).map(i => pa.get(i).getValue)

  /** The straightforward version that the optimized one replaced; both must give the same children. */
  private def referenceCross(p1: PermutedParameterArray, p2: PermutedParameterArray, rnd: Random): Seq[Double] = {
    val n = p1.size
    val i0 = rnd.nextInt(n)
    val i1 = rnd.nextInt(n)
    val slice = (math.min(i0, i1) to math.max(i0, i1)).map(p1.get)
    val sliceVals = slice.map(_.getValue).toSet
    (slice ++ p2.params.filter(p => !sliceVals.contains(p.getValue))).map(_.getValue)
  }

  private def shuffled(values: Seq[Int], seed: Long): Seq[Int] = new Random(seed).shuffle(values)

  test("the child has the same children as the straightforward version, for permutations of 0 until n") {
    for (n <- Seq(2, 3, 7, 20, 100); seed <- 1 to 25) {
      val (p1, p2) = (permutation(shuffled(0 until n, seed)), permutation(shuffled(0 until n, seed + 1000)))
      val child = OrderCrossover.cross(p1, p2, new Random(seed))
      assertResult(referenceCross(p1, p2, new Random(seed)), s"n=$n seed=$seed")(valuesOf(child))
    }
  }

  test("the same children as the straightforward version when the values are not 0 until n") {
    val ids = Seq(10, 11, 12, 13, 14, 15, 16, 17)
    for (seed <- 1 to 25) {
      val (p1, p2) = (permutation(shuffled(ids, seed)), permutation(shuffled(ids, seed + 500)))
      assertResult(referenceCross(p1, p2, new Random(seed)), s"seed=$seed")(valuesOf(OrderCrossover.cross(p1, p2, new Random(seed))))
    }
  }

  test("the same children as the straightforward version when the values are not whole numbers") {
    val doubles = Seq(0.5, 1.5, 2.5, 3.5, 4.5, 5.5)
    def of(values: Seq[Double]) = PermutedParameterArray(values.zipWithIndex.map { case (v, i) =>
      com.barrybecker4.optimization.parameter.types.DoubleParameter(v, 0.0, 10.0, "d" + i): Parameter }.toIndexedSeq, new Random(1))
    for (seed <- 1 to 15) {
      val (p1, p2) = (of(new Random(seed).shuffle(doubles)), of(new Random(seed + 9).shuffle(doubles)))
      val child = OrderCrossover.cross(p1, p2, new Random(seed))
      assertResult(referenceCross(p1, p2, new Random(seed)), s"seed=$seed")(valuesOf(child))
    }
  }

  test("the child is always a permutation of the parents' values") {
    val (p1, p2) = (permutation(shuffled(0 until 50, 3)), permutation(shuffled(0 until 50, 4)))
    for (seed <- 1 to 50) assertResult((0 until 50).map(_.toDouble))(valuesOf(OrderCrossover.cross(p1, p2, new Random(seed))).sorted)
  }

  test("parents of one parameter, or none, are returned as they are") {
    val single = permutation(Seq(0))
    assert(OrderCrossover.cross(single, single, new Random(1)) eq single)
  }

  test("a subclass that carries more than the parameters decides what the child is") {
    class Tagged(values: IndexedSeq[Parameter]) extends PermutedParameterArray(values, new Random(1)) {
      override def rebuildAfterOrderCrossover(childParams: IndexedSeq[Parameter], rnd: Random): PermutedParameterArray =
        new Tagged(childParams)
    }
    val p = new Tagged(permutation(0 until 6).params)
    assert(OrderCrossover.cross(p, p, new Random(1)).isInstanceOf[Tagged])
  }

  test("crossing through ParameterCrossover.cross gives the same child") {
    val (p1, p2) = (permutation(shuffled(0 until 12, 8)), permutation(shuffled(0 until 12, 9)))
    assertResult(valuesOf(OrderCrossover.cross(p1, p2, new Random(5))))(valuesOf(ParameterCrossover.cross(p1, p2, new Random(5))))
  }
}

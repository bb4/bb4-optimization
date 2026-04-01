// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.VariableLengthIntSet.createParam

import scala.util.Random

/**
  * Crossover operators for [[ParameterArray]] types used in this library.
  */
object ParameterCrossover {

  /**
    * Uniform crossover for [[NumericParameterArray]], order crossover (OX) for [[PermutedParameterArray]],
    * and a set-based merge for [[VariableLengthIntSet]] when both parents share the same `fullSeq`.
    */
  def cross(a: ParameterArray, b: ParameterArray, rnd: Random): ParameterArray = {
    require(a.getClass == b.getClass, "Crossover requires the same concrete ParameterArray type")
    (a, b) match {
      case (n1: NumericParameterArray, n2: NumericParameterArray) =>
        crossNumeric(n1, n2, rnd)
      case (p1: PermutedParameterArray, p2: PermutedParameterArray) =>
        crossPermuted(p1, p2, rnd)
      case (v1: VariableLengthIntSet, v2: VariableLengthIntSet) =>
        crossVariableLength(v1, v2, rnd)
      case _ =>
        throw new IllegalArgumentException("Unsupported crossover for " + a.getClass.getName)
    }
  }

  private def crossNumeric(n1: NumericParameterArray, n2: NumericParameterArray, rnd: Random): NumericParameterArray = {
    assert(n1.size == n2.size)
    val newParams = for (i <- 0 until n1.size) yield {
      if (rnd.nextBoolean()) n1.get(i) else n2.get(i)
    }
    NumericParameterArray(newParams, n1.numSteps, rnd)
  }

  /** Order crossover (OX): preserve a slice from parent1, fill from parent2 in order. */
  private def crossPermuted(p1: PermutedParameterArray, p2: PermutedParameterArray, rnd: Random): PermutedParameterArray = {
    val n = p1.size
    if (n <= 1) return p1
    val i0 = rnd.nextInt(n)
    val i1 = rnd.nextInt(n)
    val lo = math.min(i0, i1)
    val hi = math.max(i0, i1)
    val slice = (lo to hi).map(p1.get).toIndexedSeq
    val sliceVals = slice.map(_.getValue).toSet
    val restInOrder = p2.params.filter(p => !sliceVals.contains(p.getValue))
    p1.rebuildAfterOrderCrossover(slice ++ restInOrder, rnd)
  }

  private def crossVariableLength(v1: VariableLengthIntSet, v2: VariableLengthIntSet, rnd: Random): VariableLengthIntSet = {
    require(v1.fullSeq == v2.fullSeq, "VariableLengthIntSet crossover requires matching fullSeq")
    val pool = rnd.shuffle((v1.intValues ++ v2.intValues).distinct.toIndexedSeq)
    if (pool.isEmpty) return v1
    val minSz = math.max(1, math.min(v1.size, v2.size))
    val maxSz = math.min(math.max(v1.size, v2.size), v1.getMaxLength)
    val span = math.max(0, maxSz - minSz)
    val sz = minSz + (if (span == 0) 0 else rnd.nextInt(span + 1))
    val take = math.min(sz, pool.size)
    val chosen = pool.take(take)
    val params = chosen.map(createParam).toIndexedSeq
    VariableLengthIntSet.createInstance(params, v1.fullSeq, rnd)
  }
}

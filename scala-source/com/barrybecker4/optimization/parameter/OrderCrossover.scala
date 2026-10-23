// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.Parameter

import scala.collection.immutable.ArraySeq
import scala.util.Random

/**
  * Order crossover (OX) for permutations: the child starts with a random slice of the first parent, followed by
  * the remaining parameters in the order they appear in the second parent.
  *
  * The parameters of a permutation are nearly always the whole numbers 0 until n, so "is this value in the slice" is
  * answered with a boolean array instead of a set of boxed numbers. Any other values take the slower path through a set.
  */
object OrderCrossover {

  def cross(p1: PermutedParameterArray, p2: PermutedParameterArray, rnd: Random): PermutedParameterArray = {
    val n = p1.size
    if (n <= 1) return p1
    val (lo, hi) = sliceBounds(n, rnd)
    val inSlice = ValueSlice.of(p1, lo, hi)
    p1.rebuildAfterOrderCrossover(ArraySeq.unsafeWrapArray(childParams(p1, p2, lo, hi, inSlice)), rnd)
  }

  /** @return the first and last index of the slice, inclusive. */
  private def sliceBounds(n: Int, rnd: Random): (Int, Int) = {
    val first = rnd.nextInt(n)
    val second = rnd.nextInt(n)
    (math.min(first, second), math.max(first, second))
  }

  private def childParams(p1: PermutedParameterArray, p2: PermutedParameterArray,
                          lo: Int, hi: Int, inSlice: ValueSlice): Array[Parameter] = {
    val child = new Array[Parameter](p1.size)
    var next = 0
    for (i <- lo to hi) {
      child(next) = p1.get(i)
      next += 1
    }
    for (i <- 0 until p2.size if !inSlice.contains(p2.get(i).getValue)) {
      child(next) = p2.get(i)
      next += 1
    }
    child
  }
}

/** Remembers which values are in the slice that came from the first parent. */
private trait ValueSlice {
  def contains(value: Double): Boolean
}

private object ValueSlice {

  /** @return a boolean array if every value in the slice is a whole number from 0 until the size, otherwise a set. */
  def of(pa: PermutedParameterArray, lo: Int, hi: Int): ValueSlice = {
    val marks = new Array[Boolean](pa.size)
    var fitsArray = true
    for (i <- lo to hi) {
      val index = wholeNumberIndex(pa.get(i).getValue, pa.size)
      if (index < 0) fitsArray = false else marks(index) = true
    }
    if (fitsArray) new ArraySlice(marks) else new SetSlice((lo to hi).map(i => pa.get(i).getValue).toSet)
  }

  /** @return the value as an index, or -1 if it is not a whole number in 0 until size. */
  private def wholeNumberIndex(value: Double, size: Int): Int = {
    val index = value.toInt
    if (index.toDouble == value && index >= 0 && index < size) index else -1
  }

  private class ArraySlice(marks: Array[Boolean]) extends ValueSlice {
    /** A value that is not a whole number in range cannot be one of the marked ones. */
    override def contains(value: Double): Boolean = {
      val index = wholeNumberIndex(value, marks.length)
      index >= 0 && marks(index)
    }
  }

  private class SetSlice(values: Set[Double]) extends ValueSlice {
    override def contains(value: Double): Boolean = values.contains(value)
  }
}

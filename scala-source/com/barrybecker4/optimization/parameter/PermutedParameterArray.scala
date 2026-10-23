// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.distancecalculators.PermutedDistanceCalculator
import com.barrybecker4.optimization.parameter.sampling.PermutedGlobalSampler
import com.barrybecker4.optimization.parameter.types.Parameter
import scala.collection.immutable.ArraySeq
import scala.util.Random

object PermutedParameterArray {
  /**
    * Random transpositions touch about every position after 2 swaps per parameter, and 4 per parameter leave about 2%
    * in place, which is as good as random for local search. Beyond that, swapping more is wasted work.
    */
  val MAX_SWAPS_PER_PARAMETER = 4
}

/**
  * Represents a 1 dimensional array of unique permuted parameters.
  * There are no duplicates among the parameters, and this array holds them in some permuted order.
  * This sort of parameter array could be used to represent the order of cities visited in
  * the traveling salesman problem, for example.
  * @author Barry Becker
  */
case class PermutedParameterArray(params: IndexedSeq[Parameter], rnd: Random)
  extends AbstractParameterArray(params, rnd) {

  private val distanceCalculator: PermutedDistanceCalculator = new PermutedDistanceCalculator()

  def this(rnd: Random = MathUtil.RANDOM) = { this(IndexedSeq.empty[Parameter], rnd)}

  /** Permute the parameters according to the specified permutation
    * of 0 based indices.
    */
  def setPermutation(indices: List[Int]): PermutedParameterArray = {
    assert(indices.size == size)
    val newParams = for (i <- indices) yield get(i)
    PermutedParameterArray(newParams.toIndexedSeq, rnd)
  }

  /**
    * Same permutation and parameter values with a new [[scala.util.Random]] (e.g. multistart hill climbing).
    * Subclasses that carry domain state beyond [[params]] should override to preserve their concrete type.
    */
  def forkWithRnd(newRnd: Random): PermutedParameterArray =
    PermutedParameterArray(params, newRnd)

  def reverse: PermutedParameterArray = PermutedParameterArray(params.reverse, rnd)

  /**
    * The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two permutations.
    * If there are similar runs between two permutations, then the distance should be relatively small.
    *
    * N squared operation, where N is the number of params.
    * @return the distance between this parameter array and another.
    */
  override def distance(pa: ParameterArray): Double =
    distanceCalculator.findDistance(this, pa.asInstanceOf[PermutedParameterArray])

  /** Create a new permutation that is not too distant from what we have now.
    *
    * @param radius an indication of the amount of variation to use. 0 is none, 3 is a lot.
    *               Applies this many random transpositions (pairwise value swaps), in sequence:
    *               `max(1, (10 * radius * N / 100).toInt)` where `N` is [[size]].
    *               Strategies widen the radius when they are stuck, and it can get very large. Once there would be
    *               more than [[PermutedParameterArray.MAX_SWAPS_PER_PARAMETER]] swaps per parameter the result is
    *               already as good as random, so a uniform shuffle is used instead of doing all of them.
    * @return a new [[PermutedParameterArray]].
    * @note Each transposition exchanges the values at two random indices in the permutation as updated
    *       so far. Successive swaps compose like ordinary transpositions on the current state (standard for
    *       local search on permutations), rather than always reading endpoint values from the initial
    *       [[params]] snapshot.
    */
  override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
    if (size <= 1) return this
    val numToSwap = Math.max(1, (10.0 * radius * size / 100.0).toInt)
    val values = currentValues
    if (numToSwap > PermutedParameterArray.MAX_SWAPS_PER_PARAMETER * size) shuffleValues(values)
    else swapRandomPairs(values, numToSwap)
    withValues(values)
  }

  /** @return the value of each parameter, in order. Work on these primitives instead of on parameter objects. */
  private def currentValues: Array[Double] = Array.tabulate(size)(i => params(i).getValue)

  private def swapRandomPairs(values: Array[Double], numToSwap: Int): Unit =
    for (_ <- 0 until numToSwap) {
      val index1 = rnd.nextInt(size)
      var index2 = rnd.nextInt(size)
      while (index2 == index1)
        index2 = rnd.nextInt(size)
      swapAt(values, index1, index2)
    }

  /** Fisher-Yates: every ordering is equally likely. */
  private def shuffleValues(values: Array[Double]): Unit =
    for (i <- values.length - 1 until 0 by -1) swapAt(values, i, rnd.nextInt(i + 1))

  private def swapAt(values: Array[Double], i: Int, j: Int): Unit = {
    val saved = values(i)
    values(i) = values(j)
    values(j) = saved
  }

  /**
    * A new array with these values at each position. A parameter keeps its name and range, and is only replaced if its
    * value changed, so a small neighbor allocates a few parameters rather than copying all of them.
    */
  private def withValues(values: Array[Double]): PermutedParameterArray = {
    val revised = Array.tabulate[Parameter](size) { i =>
      if (params(i).getValue == values(i)) params(i) else params(i).setValue(values(i))
    }
    PermutedParameterArray(ArraySeq.unsafeWrapArray(revised), rnd)
  }

  /** Globally sample the parameter space.
    * @param requestedNumSamples approximate number of samples to retrieve. If the problem space is small
    *       and requestedNumSamples is large, it may not be possible to return this many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long) =
    new PermutedGlobalSampler(this, requestedNumSamples)

  /** @return get a completely random solution in the parameter space. */
  override def getRandomSample: ParameterArray = {
    PermutedParameterArray(rnd.shuffle(params), rnd)
  }

  /**
    * Wrap the result of order crossover (OX) in the same concrete type when subclasses carry domain state
    * beyond the raw [[params]] sequence (for example puzzle-specific paths). The default rebuilds a plain
    * [[com.barrybecker4.optimization.parameter.PermutedParameterArray]].
    */
  def rebuildAfterOrderCrossover(childParams: IndexedSeq[Parameter], rnd: Random): PermutedParameterArray =
    PermutedParameterArray(childParams, rnd)
}

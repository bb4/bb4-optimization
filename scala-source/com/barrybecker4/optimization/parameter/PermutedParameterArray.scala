// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.parameter.distancecalculators.PermutedDistanceCalculator
import com.barrybecker4.optimization.parameter.sampling.PermutedGlobalSampler
import com.barrybecker4.optimization.parameter.types.Parameter
import scala.util.Random

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
    * @return a new [[PermutedParameterArray]].
    * @note Each transposition exchanges the values at two random indices in the permutation as updated
    *       so far. Successive swaps compose like ordinary transpositions on the current state (standard for
    *       local search on permutations), rather than always reading endpoint values from the initial
    *       [[params]] snapshot.
    */
  override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
    if (size <= 1) return this
    val numToSwap = Math.max(1, (10.0 * radius * size / 100.0).toInt)

    val revisedParams = params.toArray

    for (k <- 0 until numToSwap) {
      val index1 = rnd.nextInt(size)
      var index2 = rnd.nextInt(size)
      while (index2 == index1)
        index2 = rnd.nextInt(size)
      swapValuesAt(revisedParams, index1, index2)
    }
    PermutedParameterArray(revisedParams.toIndexedSeq, rnd)
  }

  /** Swap parameter values at `i` and `j` in the working array (mutates `buf`). */
  private def swapValuesAt(buf: Array[Parameter], i: Int, j: Int): Unit = {
    val pi = buf(i)
    val pj = buf(j)
    val vi = pi.getValue
    val vj = pj.getValue
    buf(i) = pi.setValue(vj)
    buf(j) = pj.setValue(vi)
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
    * [[PermutedParameterArray]].
    */
  def rebuildAfterOrderCrossover(childParams: IndexedSeq[Parameter], rnd: Random): PermutedParameterArray =
    PermutedParameterArray(childParams, rnd)
}

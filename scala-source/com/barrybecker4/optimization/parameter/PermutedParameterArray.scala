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

  /** Up to one swap for every this many parameters is a "few", cheapest done by replacing just those parameters. */
  val FEW_SWAPS_DIVISOR = 4
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
  /**
    * True only for a plain [[PermutedParameterArray]]. A subclass usually picks its own neighbors (a puzzle path that
    * keeps its tiles legal, say) and carries more than its parameters, and these moves would bypass that and build
    * plain arrays. A subclass that wants moves overrides this, and [[applyMove]] to build its own type.
    */
  override def supportsMoves: Boolean = getClass == classOf[PermutedParameterArray]

  /**
    * The swaps that [[getRandomNeighbor]] would make, as a move. That is one [[Swap]] for the usual small radius,
    * the swaps in order for a larger one, and the swaps of a Fisher-Yates shuffle once the radius is so large that
    * a shuffle is used instead.
    */
  override def proposeMove(radius: Double): Move = {
    val swaps = if (size <= 1) IndexedSeq.empty[Swap] else proposeSwaps(numSwapsFor(radius))
    if (swaps.size == 1) swaps.head else SwapSequence(swaps)
  }

  private def proposeSwaps(numToSwap: Int): IndexedSeq[Swap] =
    if (numToSwap > PermutedParameterArray.MAX_SWAPS_PER_PARAMETER * size) shuffleSwaps
    else IndexedSeq.fill(numToSwap) {
      val index = rnd.nextInt(size)
      Swap(index, randomIndexOtherThan(index))
    }

  /** Fisher-Yates written as swaps, drawing exactly what [[shuffleValues]] draws. */
  private def shuffleSwaps: IndexedSeq[Swap] =
    (size - 1 until 0 by -1).map(i => Swap(i, rnd.nextInt(i + 1)))

  override def applyMove(move: Move): PermutedParameterArray = move match {
    case Swap(first, second) => swapPositions(IndexedSeq(Swap(first, second)))
    case SwapSequence(swaps) => swapPositions(swaps)
    case other => throw new UnsupportedOperationException("A permutation does not understand the move " + other)
  }

  /** Replace only the parameters that change when a few positions are swapped, or work on the values if there are many. */
  private def swapPositions(swaps: IndexedSeq[Swap]): PermutedParameterArray =
    if (swaps.size * PermutedParameterArray.FEW_SWAPS_DIVISOR <= size) {
      val revised = params.toArray
      swaps.foreach(s => swapValuesAt(revised, s.first, s.second))
      PermutedParameterArray(ArraySeq.unsafeWrapArray(revised), rnd)
    } else {
      val values = currentValues
      swaps.foreach(s => swapAt(values, s.first, s.second))
      withValues(values)
    }

  override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
    if (size <= 1) return this
    val numToSwap = numSwapsFor(radius)
    if (numToSwap * PermutedParameterArray.FEW_SWAPS_DIVISOR <= size) swapFew(numToSwap)
    else remixMany(numToSwap)
  }

  /**
    * @return the swaps the radius asks for, but never more than one past the most that are worth doing. The radius can
    *         be astronomically large when a strategy keeps widening it, so this is clamped before it becomes an Int.
    */
  private def numSwapsFor(radius: Double): Int = {
    val wanted = 10.0 * radius * size / 100.0
    val mostWorthDoing = PermutedParameterArray.MAX_SWAPS_PER_PARAMETER * size
    Math.max(1, Math.min(wanted, mostWorthDoing + 1.0).toInt)
  }

  /** The usual case, a handful of swaps: copy the parameter references once and replace only the swapped ones. */
  private def swapFew(numToSwap: Int): PermutedParameterArray = {
    val revised = params.toArray
    for (_ <- 0 until numToSwap) {
      val index = rnd.nextInt(size)
      swapValuesAt(revised, index, randomIndexOtherThan(index))
    }
    PermutedParameterArray(ArraySeq.unsafeWrapArray(revised), rnd)
  }

  /** Many swaps would replace most parameters several times over, so work on the plain values and replace each once. */
  private def remixMany(numToSwap: Int): PermutedParameterArray = {
    val values = currentValues
    if (numToSwap > PermutedParameterArray.MAX_SWAPS_PER_PARAMETER * size) shuffleValues(values)
    else swapRandomPairs(values, numToSwap)
    withValues(values)
  }

  private def randomIndexOtherThan(index: Int): Int = {
    var other = rnd.nextInt(size)
    while (other == index) other = rnd.nextInt(size)
    other
  }

  /** Swap parameter values at `i` and `j` in the working array (mutates `buf`). Each parameter keeps its name and range. */
  private def swapValuesAt(buf: Array[Parameter], i: Int, j: Int): Unit = {
    val (pi, pj) = (buf(i), buf(j))
    val (vi, vj) = (pi.getValue, pj.getValue)
    buf(i) = pi.setValue(vj)
    buf(j) = pj.setValue(vi)
  }

  /** @return the value of each parameter, in order. Work on these primitives instead of on parameter objects. */
  private def currentValues: Array[Double] = {
    val values = new Array[Double](size)
    var i = 0
    while (i < values.length) {
      values(i) = params(i).getValue
      i += 1
    }
    values
  }

  private def swapRandomPairs(values: Array[Double], numToSwap: Int): Unit =
    for (_ <- 0 until numToSwap) {
      val index = rnd.nextInt(size)
      swapAt(values, index, randomIndexOtherThan(index))
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

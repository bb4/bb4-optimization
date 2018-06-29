// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.distancecalculators.{DistanceCalculator, PermutedDistanceCalculator}
import com.barrybecker4.optimization.parameter.improvement.DiscreteImprovementFinder
import com.barrybecker4.optimization.parameter.improvement.Improvement
import com.barrybecker4.optimization.parameter.sampling.PermutedGlobalSampler
import com.barrybecker4.optimization.parameter.types.Parameter

import scala.collection.mutable
import scala.util.Random

/**
  * Represents a 1 dimensional array of unique permuted parameters.
  * There are no duplicates among the parameters, and this array holds them in some permuted order.
  * This sort of parameter array could be used to represent the order of cities visited in
  * the traveling salesman problem, for example.
  * @author Barry Becker
  */
class PermutedParameterArray(theParams: Array[Parameter], rnd: Random = MathUtil.RANDOM)
  extends AbstractParameterArray(theParams) {

  private val distanceCalculator: PermutedDistanceCalculator = new PermutedDistanceCalculator()

  def this() { this(Array[Parameter]())}

  /** Permute the parameters according to the specified permutation
    * of 0 based indices.
    */
  def setPermutation(indices: List[Integer]): Unit = {
    assert(indices.size == size)
    val newParams = for (i <- indices) yield get(i)
    params = newParams.toArray
  }

  override protected def createInstance = new PermutedParameterArray

  def reverse: ParameterArray = {
    val paramCopy = this.copy
    val len = size
    for (i <- 0 until len / 2) {
      val temp = paramCopy.params(i)
      paramCopy.params(i) = paramCopy.params(len - i - 1)
      paramCopy.params(len - i - 1) = temp
    }
    paramCopy
  }

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
    * @param radius an indication of the amount of variation to use. 0 is none, 3 is a lot.
    *        Change Math.min(1, 10 * radius * N/100) of the entries, where N is the number of params
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
    if (size <= 1) return this
    val numToSwap = Math.max(1, (10.0 * radius * size / 100.0).toInt)
    val nbr = this.copy.asInstanceOf[PermutedParameterArray]
    for (k <- 0 until numToSwap) {
      val index1 = rnd.nextInt(size)
      var index2 = rnd.nextInt(size)
      while ( {
        index2 == index1
      }) index2 = rnd.nextInt(size)
      val temp = nbr.params(index1)
      nbr.params(index1) = nbr.params(index2)
      nbr.params(index2) = temp
    }
    nbr
  }

  /**
    * Globally sample the parameter space.
    * @param requestedNumSamples approximate number of samples to retrieve. If the problem space is small
    *       and requestedNumSamples is large, it may not be possible to return this many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long) = new PermutedGlobalSampler(this, requestedNumSamples)

  /** Try swapping parameters randomly until we find an improvement (if we can);  */
  override def findIncrementalImprovement(optimizee: Optimizee,
            jumpSize: Double, lastImprovement: Improvement, cache: mutable.Set[ParameterArray]): Improvement = {
    val finder = new DiscreteImprovementFinder(this)
    finder.findIncrementalImprovement(optimizee, jumpSize, cache)
  }

  /** @return get a completely random solution in the parameter space. */
  override def getRandomSample: ParameterArray = {
    val theParams: Array[Parameter] = rnd.shuffle(params.toSeq).toArray
    new PermutedParameterArray(theParams)
  }
}
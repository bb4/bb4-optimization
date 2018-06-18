// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling

import java.math.BigInteger

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.common.math.combinatorics.Permuter
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.parameter.PermutedParameterArray
import PermutedGlobalSampler.CLOSE_FACTOR
import scala.collection.mutable.ArrayBuffer


object PermutedGlobalSampler {

  /** If the requestedNumSamples is within this percent of the total, then use exhaustive search */
  private val CLOSE_FACTOR = 60
}

/**
  * Finds a set of uniformly distributed global samples in a large numeric parameter space.
  * If the number of samples requested is really large, then all possible values will be returned.
  * @param params an array of params to initialize with.
  * @param requestedNumSamples desired number of samples to retrieve. If very large, will get all of them.
  * @author Barry Becker
  */
class PermutedGlobalSampler(var params: PermutedParameterArray, val requestedNumSamples: Long)
    extends AbstractGlobalSampler[PermutedParameterArray] {

  // See page 13 in How to Solve It.
  // Divide by 2 because it does not matter which param we start with.
  val numPermutations: BigInteger = MathUtil.bigFactorial(params.size).divide(BigInteger.valueOf(2))

  // if the requested number of samples is close to the total number of permutations,
  // then we could just enumerate the permutations.
  numSamples = requestedNumSamples

  /** becomes true if the requestedNumSamples is close to the total number of permutations in the space */
  private var useExhaustiveSearch = {
    val v: BigInteger = numPermutations.multiply(BigInteger.valueOf(CLOSE_FACTOR)).divide(BigInteger.valueOf(100))
    BigInteger.valueOf(requestedNumSamples).compareTo(v) == 1
  }

  /** Used to enumerate all possible permutations when doing exhaustive search */
  private var permuter: Permuter = _

  if (useExhaustiveSearch)
    permuter = new Permuter(params.size)

  /** used to cache the samples already tried so we do not repeat them if the requestedNumSamples is small */
  private val globalSamples = new ArrayBuffer[ParameterArray]()


  override def next: PermutedParameterArray = {
    if (counter >= numSamples) throw new IllegalStateException("ran out of samples.")
    if (counter == numSamples - 1) hasNext = false
    counter += 1
    if (useExhaustiveSearch) getNextExhaustiveSample
    else getNextRandomSample
  }

  /** Randomly sample the parameter space until a sample that was not seen before is found.
    * @return the next random sample.
    */
  private def getNextRandomSample = {
    var nextSample: PermutedParameterArray = null
    while (globalSamples.size < counter) {
      nextSample = params.getRandomSample.asInstanceOf[PermutedParameterArray]
      if (!globalSamples.contains(nextSample))
        globalSamples.append(nextSample)
    }
    nextSample
  }

  /** Globally sample the parameter space searching all possibilities.
    * @return the next exhaustive sample.
    */
  private def getNextExhaustiveSample = {
    val pParams = params.copy.asInstanceOf[PermutedParameterArray]
    pParams.setPermutation(permuter.next)
    hasNext = permuter.hasNext
    pParams
  }
}
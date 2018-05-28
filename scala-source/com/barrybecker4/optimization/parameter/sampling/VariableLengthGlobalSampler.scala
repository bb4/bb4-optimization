// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.common.math.combinatorics.Combinater
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.parameter.VariableLengthIntArray

import scala.collection.JavaConversions
import java.util.NoSuchElementException

import scala.collection.mutable.ArrayBuffer


object VariableLengthGlobalSampler {
  /** If the requestedNumSamples is within this percent of the total, then use exhaustive search */
  private val CLOSE_FACTOR = 0.5
}

/**
  * Finds a set of uniformly distributed global samples in a large numeric parameter space.
  * If the number of samples requested is really large, then all possible values will be returned.
  * @param params an array of params to initialize with.
  * @param requestedNumSamples desired number of samples to retrieve. If very large, will get all of them.
  * @author Barry Becker
  */
class VariableLengthGlobalSampler(var params: VariableLengthIntArray, val requestedNumSamples: Long)
    extends AbstractGlobalSampler[VariableLengthIntArray] {

  var totalConfigurations: Long = Long.MaxValue
  if (params.getMaxLength <= 60) totalConfigurations = Math.pow(2.0, params.getMaxLength).toLong
  // if the requested number of samples is close to the total number of configurations,
  // then just search through all possible configurations.
  numSamples = requestedNumSamples
  useExhaustiveSearch = requestedNumSamples > VariableLengthGlobalSampler.CLOSE_FACTOR * totalConfigurations

  private var combinater: Combinater = _
  if (useExhaustiveSearch)
    combinater = new Combinater(params.getMaxLength)

  /** used to cache the samples already tried so we do not repeat them if the requestedNumSamples is small */
  private[sampling] val globalSamples = new ArrayBuffer[ParameterArray]()
  /** Used to enumerate all possible permutations when doing exhaustive search */

  /** becomes true if the requestedNumSamples is close to the total number of permutations in the space */
  private var useExhaustiveSearch = false

  override def next: VariableLengthIntArray = {
    if (counter >= numSamples) throw new NoSuchElementException("ran out of samples.")
    if (counter == numSamples - 1) hasNext = false
    counter += 1
    if (useExhaustiveSearch) getNextExhaustiveSample
    else getNextRandomSample
  }

  /**
    * Randomly sample the parameter space until a sample that was not seen before is found.
    *
    * @return the next random sample.
    */
  private def getNextRandomSample = {
    var nextSample: VariableLengthIntArray = null
    while (globalSamples.size < counter) {
      nextSample = params.getRandomSample.asInstanceOf[VariableLengthIntArray]
      if (!globalSamples.contains(nextSample)) globalSamples.append(nextSample)
    }
    nextSample
  }

  /** Globally sample the parameter space searching all possibilities.
    * @return the next exhaustive sample.
    */
  private def getNextExhaustiveSample = {
    val vlParams = params.copy.asInstanceOf[VariableLengthIntArray]
    vlParams.setCombination(combinater.next.map(_.toInt))
    hasNext = combinater.hasNext
    vlParams
  }
}
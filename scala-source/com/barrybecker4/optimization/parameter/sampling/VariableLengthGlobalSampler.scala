// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.common.math.combinatorics.Combinater
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.parameter.VariableLengthIntSet
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
class VariableLengthGlobalSampler(var params: VariableLengthIntSet, val requestedNumSamples: Long)
    extends AbstractGlobalSampler[VariableLengthIntSet] {

  /** used to cache the samples already tried so we do not repeat them if the requestedNumSamples is small */
  private[sampling] val globalSamples = new ArrayBuffer[ParameterArray]()

  /** becomes true if the requestedNumSamples is close to the total number of permutations in the space */
  private var useExhaustiveSearch = false

  var totalConfigurations: Long = Long.MaxValue
  if (params.getMaxLength <= 60) totalConfigurations = Math.pow(2.0, params.getMaxLength).toLong
  // if the requested number of samples is close to the total number of configurations,
  // then just search through all possible configurations.
  numSamples = requestedNumSamples

  useExhaustiveSearch = requestedNumSamples > VariableLengthGlobalSampler.CLOSE_FACTOR * totalConfigurations

  /** Used to enumerate all possible combination when doing exhaustive search */
  private var combinator: Combinater = _
  if (useExhaustiveSearch)
    combinator = new Combinater(params.getMaxLength)


  override def next: VariableLengthIntSet = {
    if (counter >= numSamples) throw new NoSuchElementException("ran out of samples.")
    if (counter == numSamples - 1) hasNext = false
    counter += 1
    if (useExhaustiveSearch) getNextExhaustiveSample
    else getNextRandomSample
  }

  /** Randomly sample the parameter space until a sample that was not seen before is found.
    * @return the next random sample.
    */
  private def getNextRandomSample = {
    var nextSample: VariableLengthIntSet = null
    while (globalSamples.size < counter) {
      nextSample = params.getRandomSample.asInstanceOf[VariableLengthIntSet]
      if (!globalSamples.contains(nextSample))
        globalSamples.append(nextSample)
    }
    nextSample
  }

  /** Globally sample the parameter space searching all possibilities.
    * @return the next exhaustive sample.
    */
  private def getNextExhaustiveSample = {
    val theNext = combinator.next()
    val v1Params = params.getCombination(theNext.map(_.toInt))
    hasNext = combinator.hasNext
    v1Params
  }
}
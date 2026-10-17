// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.math.multiarray.MultiDimensionalIndexer
import com.barrybecker4.optimization.parameter.NumericParameterArray
import java.util.NoSuchElementException


/**
  * Finds a set of uniformly distributed global samples in a large numeric parameter space.
  * @param params an array of params to initialize with.
  * @author Barry Becker
  */
class NumericGlobalSampler(var params: NumericParameterArray, val requestedNumSamples: Long)
    extends AbstractGlobalSampler[NumericParameterArray] {

  private val dims = new Array[Int](params.size)

  /** number of discrete samples to take along each parameter */
  private val samplingRate = determineSamplingRate(requestedNumSamples)
  for (i <- dims.indices) {
    dims(i) = samplingRate
  }
  // this potentially takes a lot of memory - may need to revisit
  private val samples = new MultiDimensionalIndexer(dims)
  numSamples = samples.getNumValues

  override def next(): NumericParameterArray = {
    if (counter >= numSamples) throw new NoSuchElementException("ran out of samples.")
    if (counter == numSamples - 1) hasNext = false
    val index = samples.getIndexFromRaw(counter)
    val paramList =
      for (j <- 0 until params.size) yield {
        val p = params.get(j)
        val increment = (p.maxValue - p.minValue) / samplingRate
        p.setValue(p.minValue + increment / 2.0 + index(j) * increment)
      }
    counter += 1
    NumericParameterArray(paramList, params.numSteps, params.rnd)
  }

  private def determineSamplingRate(requestedNumSamples: Long) = {
    if (requestedNumSamples < 1)
      throw new IllegalArgumentException(
        s"requestedNumSamples must be positive, got $requestedNumSamples")
    val numDims = math.max(1, params.size)
    val raw = math.pow(requestedNumSamples.toDouble, 1.0 / numDims)
    // MultiDimensionalIndexer multiplies the per-dimension counts in a 32-bit int.
    // A request such as Long.MaxValue overflows that product (2D wraps to 1 sample;
    // 1D asks for two billion). Fail instead of searching a corrupted grid.
    val maxSafePerDim = math.max(1, math.floor(math.pow(Int.MaxValue.toDouble, 1.0 / numDims)).toInt)
    if (raw.isNaN || raw > maxSafePerDim)
      throw new IllegalArgumentException(
        s"Requested global sample grid is too large: about ${raw} points per dimension across $numDims dimensions " +
          s"(limit $maxSafePerDim so the grid count fits in a 32-bit int)")
    math.max(1, raw.toInt)
  }
}
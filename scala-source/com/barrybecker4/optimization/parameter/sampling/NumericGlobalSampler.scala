// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling

import com.barrybecker4.common.math.MultiDimensionalIndexer
import com.barrybecker4.optimization.parameter.NumericParameterArray
import java.util.NoSuchElementException


/**
  * Finds a set of uniformly distributed global samples in a large numeric parameter space.
  * @param params an array of params to initialize with.
  * @author Barry Becker
  */
class NumericGlobalSampler(var params: NumericParameterArray, val requestedNumSamples: Long)
    extends AbstractGlobalSampler[NumericParameterArray] {

  val dims = new Array[Int](params.size)

  /** number of discrete samples to take along each parameter */
  private var samplingRate = determineSamplingRate(requestedNumSamples)
  for (i <- dims.indices) {
    dims(i) = samplingRate
  }
  // this potentially takes a lot of memory - may need to revisit
  private var samples = new MultiDimensionalIndexer(dims)
  numSamples = samples.getNumValues


  override def next: NumericParameterArray = {
    if (counter >= numSamples) throw new NoSuchElementException("ran out of samples.")
    if (counter == numSamples - 1) hasNext = false
    val index = samples.getIndexFromRaw(counter.toInt)
    val nextSample = params.copy
    for (j <- 0 until nextSample.size) {
      val p = nextSample.get(j)
      val increment = (p.maxValue - p.minValue) / samplingRate
      p.setValue(p.minValue + increment / 2.0 + index(j) * increment)
    }
    counter += 1
    nextSample
  }

  private def determineSamplingRate(requestedNumSamples: Long) = {
    val numDims = params.size
    Math.pow(requestedNumSamples.toDouble, 1.0 / numDims).toInt
  }
}
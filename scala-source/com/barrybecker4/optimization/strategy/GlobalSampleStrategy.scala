// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable
import GlobalSampleStrategy._


object GlobalSampleStrategy {
  /** Some number of samples to try.  */
  private val DEFAULT_NUM_SAMPLES = 10000
}

/**
  * Global sampling optimization strategy.
  * No log file specified in this constructor. (use this version if running in unsigned applet).
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class GlobalSampleStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** the user should set this explicitly. */
  private var numSample = DEFAULT_NUM_SAMPLES

  /**
    * Requested sample budget for numeric spaces: [[com.barrybecker4.optimization.parameter.sampling.NumericGlobalSampler]]
    * chooses about `floor(requested^(1/numDims))` grid points per dimension (total samples ≈ that value raised to `numDims`).
    * For other [[com.barrybecker4.optimization.parameter.ParameterArray]] types, semantics follow their `findGlobalSamples` implementation.
    */
  def setSamplingRate(samplingRate: Int): Unit = {
    assert(samplingRate > 0)
    numSample = samplingRate
  }

  /**
    * Sparsely sample the global space and return the best of the samples.
    * If the number of dimensions is large, you must use a very small number of samples per dimension
    * since the number of samples tested is equal to samplesPerDim ^ numDims.
    * For example if you have 8 dimensions and samplesPerDim = 4, then the
    * number of samples checked will be 4^8 = 65,536
    * If the number of samples for a dimension is 3 then the samples look like the following:
    * Min|--X----X----X--|Max
    *
    * Doing this sampling before pursuing a search strategy increases the chance
    * that you will find the global maxima. It does not guarantee it, because the space
    * you are sampling may have a high frequency of peaks and valleys.
    *
    * @param params       the params to compare evaluation against if we evaluate BY_COMPARISON.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return best solution found using global sampling.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    val samples = params.findGlobalSamples(numSample)
    var bestParams = ParameterArrayWithFitness(params, Double.MaxValue)
    var done = false

    while (samples.hasNext && !done) {
      val sample = samples.next()
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(sample, params)
        else optimizee.evaluateFitness(sample)
      //sample.setFitness(fitness)
      //println( "key = " + hashKey + '\n' + testParams + "\n  fitness=" + fitness );
      if (fitness < bestParams.fitness) {
        bestParams = ParameterArrayWithFitness(sample, fitness)
        notifyOfChange(bestParams)
      }
      if (isOptimalFitnessReached(bestParams))
        done = true
    }
    bestParams
  }

  /**
    * Same sampling and evaluation as [[doOptimization]], but retains the `k` best distinct samples
    * (lowest fitness first). Used for multistart local search.
    */
  private[strategy] def doOptimizationTopK(params: ParameterArray, fitnessRange: Double, k: Int)
    : Seq[ParameterArrayWithFitness] = {
    val kClamped = math.max(1, k)
    val samples = params.findGlobalSamples(numSample)
    var globalBest = ParameterArrayWithFitness(params, Double.MaxValue)
    var done = false
    val worstIsHead: Ordering[ParameterArrayWithFitness] =
      Ordering.by[ParameterArrayWithFitness, Double](_.fitness).reverse
    val pq = mutable.PriorityQueue.empty[ParameterArrayWithFitness](using worstIsHead)

    while (samples.hasNext && !done) {
      val sample = samples.next()
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(sample, params)
        else optimizee.evaluateFitness(sample)
      val candidate = ParameterArrayWithFitness(sample, fitness)
      if (fitness < globalBest.fitness) {
        globalBest = candidate
        notifyOfChange(globalBest)
      }
      if (pq.size < kClamped) pq.enqueue(candidate)
      else if (fitness < pq.head.fitness) {
        pq.dequeue()
        pq.enqueue(candidate)
      }
      if (isOptimalFitnessReached(globalBest))
        done = true
    }
    if (globalBest.fitness == Double.MaxValue) IndexedSeq.empty
    else {
      val sorted = pq.toArray.sortBy(_.fitness).toIndexedSeq
      // Prepend first-seen global best so tie-breaking matches [[doOptimization]]; multistart's first local run matches legacy.
      // Do not dedupe by set equality: distinct ParameterArray instances share one Random; deduping would drop repeats but
      // leave later seeds in the list, consuming the RNG before the primary hill climb and changing its trajectory.
      val merged = globalBest +: sorted.filterNot(_.pa eq globalBest.pa)
      merged.take(kClamped)
    }
  }
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable
import GlobalSampleStrategy._


object GlobalSampleStrategy {
  /** Some number of samples to try.  */
  private val DEFAULT_NUM_SAMPLES = 10000

  private val DISTANCE_SCALE_SAMPLES = 5
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
    val top = doOptimizationTopK(params, fitnessRange, k = 1, SeedSelection.TopFitness)
    top.headOption.getOrElse(ParameterArrayWithFitness(params, Double.MaxValue))
  }

  /**
    * Same sampling and evaluation as [[doOptimization]], but retains multistart seeds.
    * Used for multistart local search.
    */
  private[strategy] def doOptimizationTopK(
      params: ParameterArray,
      fitnessRange: Double,
      k: Int,
      seedSelection: SeedSelection = SeedSelection.TopFitness
  ): Seq[ParameterArrayWithFitness] = {
    val kClamped = math.max(1, k)
    val samples = params.findGlobalSamples(numSample)
    var globalBest = ParameterArrayWithFitness(params, Double.MaxValue)
    var done = false
    val worstIsHead: Ordering[ParameterArrayWithFitness] =
      Ordering.by[ParameterArrayWithFitness, Double](_.fitness).reverse

    val reservoirSize = seedSelection match {
      case SeedSelection.TopFitness =>
        kClamped
      case SeedSelection.DiverseFromTopM(topM, _) =>
        math.max(topM, kClamped)
    }
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
      if (pq.size < reservoirSize) pq.enqueue(candidate)
      else if (fitness < pq.head.fitness) {
        pq.dequeue()
        pq.enqueue(candidate)
      }
      if (isOptimalFitnessReached(globalBest))
        done = true
    }
    if (globalBest.fitness == Double.MaxValue) IndexedSeq.empty
    else {
      seedSelection match {
        case SeedSelection.TopFitness =>
          topFitnessSeeds(globalBest, pq, kClamped)
        case diverse: SeedSelection.DiverseFromTopM =>
          diverseSeeds(globalBest, pq.toArray.sortBy(_.fitness), kClamped, params, diverse.minSeparation)
      }
    }
  }

  private def topFitnessSeeds(
      globalBest: ParameterArrayWithFitness,
      pq: mutable.PriorityQueue[ParameterArrayWithFitness],
      k: Int
  ): Seq[ParameterArrayWithFitness] = {
    val sorted = pq.toArray.sortBy(_.fitness).toIndexedSeq
    // Prepend first-seen global best so tie-breaking matches [[doOptimization]]; multistart's first local run matches legacy.
    // Do not dedupe by set equality: distinct ParameterArray instances share one Random; deduping would drop repeats but
    // leave later seeds in the list, consuming the RNG before the primary hill climb and changing its trajectory.
    val merged = globalBest +: sorted.filterNot(_.pa eq globalBest.pa)
    merged.take(k)
  }

  private def diverseSeeds(
      globalBest: ParameterArrayWithFitness,
      reservoir: Array[ParameterArrayWithFitness],
      k: Int,
      reference: ParameterArray,
      minSeparation: Double
  ): Seq[ParameterArrayWithFitness] = {
    val scale = typicalDistanceScale(reference)
    val selected = mutable.ArrayBuffer[ParameterArrayWithFitness](globalBest)
    var pool = reservoir.filterNot(_.pa eq globalBest.pa).to(mutable.ArrayBuffer)

    while (selected.size < k && pool.nonEmpty) {
      def minNormDist(c: ParameterArrayWithFitness): Double =
        selected.map(s => c.pa.distance(s.pa) / scale).min

      val separated = pool.filter(c => minNormDist(c) >= minSeparation - 1.0e-12)
      val candidates = if (separated.nonEmpty) separated else pool
      val anchor = selected.head
      val maxDist = candidates.map(minNormDist).max
      val tier = candidates.filter(c => minNormDist(c) >= maxDist - 1.0e-12)
      val opposite = tier.filter(c => oppositeBasinBonus(anchor, c) > 0)
      val pickFrom = if (opposite.nonEmpty) opposite else tier
      val next = pickFrom.maxBy(c => (minNormDist(c), oppositeBasinBonus(anchor, c)))
      selected += next
      pool = pool.filterNot(_ eq next)
    }
    selected.toIndexedSeq
  }

  /** Prefer seeds on the opposite side of the origin on the first parameter when min-distance ties. */
  private def oppositeBasinBonus(anchor: ParameterArrayWithFitness, candidate: ParameterArrayWithFitness): Int =
    if (anchor.pa.size != candidate.pa.size || anchor.pa.size == 0) 0
    else {
      val ax = anchor.pa.get(0).getValue
      val cx = candidate.pa.get(0).getValue
      if (ax * cx < 0) 1 else 0
    }

  private def typicalDistanceScale(reference: ParameterArray): Double = {
    var sum = 0.0
    for (_ <- 0 until DISTANCE_SCALE_SAMPLES) {
      sum += reference.distance(reference.getRandomSample)
    }
    val avg = sum / DISTANCE_SCALE_SAMPLES
    if (avg > 1.0e-12) avg else 1.0
  }
}

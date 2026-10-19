// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import GlobalSampleStrategy.*
import com.barrybecker4.optimization.strategy.hillclimbing.SeedSelection


object GlobalSampleStrategy {
  /** Some number of samples to try.  */
  private val DEFAULT_NUM_SAMPLES = 10000

  private val DISTANCE_SCALE_SAMPLES = 5

  /** Distances closer than this are treated as equal when choosing between seed candidates. */
  private val TIE_EPS = 1.0e-12
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
    require(samplingRate > 0, s"samplingRate must be positive, got $samplingRate")
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
    * A sample whose fitness is NaN is never a seed.
    */
  private[strategy] def doOptimizationTopK(
      params: ParameterArray,
      fitnessRange: Double,
      k: Int,
      seedSelection: SeedSelection = SeedSelection.TopFitness
  ): Seq[ParameterArrayWithFitness] = {
    val kClamped = math.max(1, k)
    val reservoir = new SeedReservoir(reservoirSize(seedSelection, kClamped))
    val globalBest = sampleInto(reservoir, params)
    if (globalBest.fitness == Double.MaxValue) IndexedSeq.empty
    else {
      seedSelection match {
        case SeedSelection.TopFitness =>
          topFitnessSeeds(globalBest, reservoir, kClamped)
        case diverse: SeedSelection.DiverseFromTopM =>
          diverseSeeds(globalBest, reservoir.bestFirst, kClamped, params, diverse.minSeparation)
      }
    }
  }

  private def reservoirSize(seedSelection: SeedSelection, k: Int): Int =
    seedSelection match {
      case SeedSelection.TopFitness => k
      case SeedSelection.DiverseFromTopM(topM, _) => math.max(topM, k)
    }

  /** Evaluates the samples in turn, stopping early at the optimal fitness. @return the best one, in order seen. */
  private def sampleInto(reservoir: SeedReservoir, params: ParameterArray): ParameterArrayWithFitness = {
    val samples = params.findGlobalSamples(numSample)
    var globalBest = ParameterArrayWithFitness(params, Double.MaxValue)
    while (samples.hasNext && !isOptimalFitnessReached(globalBest)) {
      val sample = samples.next()
      val candidate = ParameterArrayWithFitness(sample, scoreAgainst(sample, params))
      if (candidate.fitness < globalBest.fitness) {
        globalBest = candidate
        notifyOfChange(globalBest)
      }
      reservoir.offer(candidate)
    }
    globalBest
  }

  /** Keeps the `capacity` lowest-fitness candidates offered so far. */
  private final class SeedReservoir(capacity: Int) {

    // the head of the queue is the largest fitness, i.e. the first one to be pushed out
    private val worstOnTop = mutable.PriorityQueue.empty[ParameterArrayWithFitness](
      using Ordering.by[ParameterArrayWithFitness, Double](_.fitness))

    def offer(candidate: ParameterArrayWithFitness): Unit =
      if (worstOnTop.size < capacity) worstOnTop.enqueue(candidate)
      else if (candidate.fitness < worstOnTop.head.fitness) {
        worstOnTop.dequeue()
        worstOnTop.enqueue(candidate)
      }

    def bestFirst: Array[ParameterArrayWithFitness] = worstOnTop.toArray.sortBy(_.fitness)
  }

  private def topFitnessSeeds(
      globalBest: ParameterArrayWithFitness,
      reservoir: SeedReservoir,
      k: Int
  ): Seq[ParameterArrayWithFitness] = {
    // Prepend first-seen global best so tie-breaking matches [[doOptimization]]; multistart's first local run matches legacy.
    // Do not dedupe by set equality: distinct ParameterArray instances share one Random; deduping would drop repeats but
    // leave later seeds in the list, consuming the RNG before the primary hill climb and changing its trajectory.
    val merged = globalBest +: reservoir.bestFirst.toIndexedSeq.filterNot(_.pa eq globalBest.pa)
    merged.take(k)
  }

  /** A seed candidate and its distance (in units of the typical distance) to the nearest seed chosen so far. */
  private final class PoolEntry(val candidate: ParameterArrayWithFitness, var nearestSeedDistance: Double)

  /**
    * Greedy max-min selection: the global best first, then repeatedly the pool member that is farthest from all seeds
    * chosen so far (preferring ones that are at least `minSeparation` away, and then ones on the other side of the
    * origin from the first seed).
    */
  private def diverseSeeds(
      globalBest: ParameterArrayWithFitness,
      reservoir: Array[ParameterArrayWithFitness],
      k: Int,
      reference: ParameterArray,
      minSeparation: Double
  ): Seq[ParameterArrayWithFitness] = {
    val scale = typicalDistanceScale(reference)
    def distance(a: ParameterArrayWithFitness, b: ParameterArrayWithFitness): Double = a.pa.distance(b.pa) / scale

    val selected = ArrayBuffer[ParameterArrayWithFitness](globalBest)
    val pool = reservoir.filterNot(_.pa eq globalBest.pa)
      .map(c => new PoolEntry(c, distance(c, globalBest))).to(ArrayBuffer)

    while (selected.size < k && pool.nonEmpty) {
      val next = pickNext(globalBest, pool, minSeparation)
      selected += next.candidate
      pool.filterInPlace(_ ne next)
      // keeping the nearest-seed distance up to date costs one distance per pool member per pick
      pool.foreach(e => e.nearestSeedDistance = math.min(e.nearestSeedDistance, distance(e.candidate, next.candidate)))
    }
    selected.toIndexedSeq
  }

  private def pickNext(anchor: ParameterArrayWithFitness, pool: ArrayBuffer[PoolEntry], minSeparation: Double): PoolEntry = {
    val separated = pool.filter(_.nearestSeedDistance >= minSeparation - TIE_EPS)
    val candidates = if (separated.nonEmpty) separated else pool
    val maxDist = candidates.map(_.nearestSeedDistance).max
    val farthest = candidates.filter(_.nearestSeedDistance >= maxDist - TIE_EPS)
    val opposite = farthest.filter(e => oppositeBasinBonus(anchor, e.candidate) > 0)
    val pickFrom = if (opposite.nonEmpty) opposite else farthest
    pickFrom.maxBy(e => (e.nearestSeedDistance, oppositeBasinBonus(anchor, e.candidate)))
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

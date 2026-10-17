// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

/**
  * A strategy which naively tries all possibilities.
  * This will not be practical for problems with real valued parameters where the search space is infinite.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class BruteForceStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** Refuse integer lattices larger than this. Bigger spaces will not finish. */
  private val MaxIntegerCandidates = 1000000L

  /** Systematically search the entire global space and return the best of the samples.
    * Stops if the optimal fitness is reached.
    * @param params       the params to compare evaluation against if we evaluate BY_COMPARISON.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return best solution found using global sampling.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness =
    params match {
      case numeric: NumericParameterArray if hasContinuousParameter(numeric) =>
        throw new IllegalArgumentException(
          "BRUTE_FORCE cannot enumerate a continuous parameter space. " +
            "Use GLOBAL_SAMPLING or another strategy for real-valued parameters.")
      case numeric: NumericParameterArray =>
        enumerateIntegerLattice(numeric, params)
      case _ =>
        searchSamples(params)
    }

  private def hasContinuousParameter(params: NumericParameterArray): Boolean =
    (0 until params.size).exists(i => !params.get(i).isIntegerOnly)

  /** Every inclusive integer combination. Stops early when the known optimum is reached. */
  private def enumerateIntegerLattice(template: NumericParameterArray,
                                      baseline: ParameterArray): ParameterArrayWithFitness = {
    val bounds = (0 until template.size).map { i =>
      val p = template.get(i)
      (p.minValue.round.toInt, p.maxValue.round.toInt)
    }
    var product = 1L
    bounds.foreach { case (lo, hi) =>
      val span = (hi - lo + 1).toLong
      if (span <= 0 || product > MaxIntegerCandidates / span)
        throw new IllegalArgumentException(
          s"BRUTE_FORCE integer space has more than $MaxIntegerCandidates candidates")
      product *= span
    }

    var best = ParameterArrayWithFitness(template, Double.MaxValue)

    def consider(values: IndexedSeq[Double]): Unit = {
      val candidate = NumericParameterArray(
        values.indices.map(i => template.get(i).setValue(values(i))),
        template.numSteps,
        template.rnd
      )
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(candidate, baseline)
        else optimizee.evaluateFitness(candidate)
      if (fitness < best.fitness) {
        best = ParameterArrayWithFitness(candidate, fitness)
        notifyOfChange(best)
      }
    }

    val coords = Array.fill(bounds.size)(0.0)
    def rec(dim: Int): Unit = {
      if (isOptimalFitnessReached(best)) return
      if (dim == bounds.size) consider(coords.toIndexedSeq)
      else {
        val (lo, hi) = bounds(dim)
        var v = lo
        while (v <= hi && !isOptimalFitnessReached(best)) {
          coords(dim) = v.toDouble
          rec(dim + 1)
          v += 1
        }
      }
    }
    if (bounds.isEmpty) consider(IndexedSeq.empty)
    else rec(0)
    best
  }

  private def searchSamples(params: ParameterArray): ParameterArrayWithFitness = {
    val samples = params.findGlobalSamples(Long.MaxValue)
    var bestParams: ParameterArrayWithFitness = ParameterArrayWithFitness(params, Double.MaxValue)
    var done = false
    while (samples.hasNext && !done) {
      val sample = samples.next()
      val fitness =
        if (optimizee.evaluateByComparison) optimizee.compareFitness(sample, params)
        else optimizee.evaluateFitness(sample)
      if (fitness < bestParams.fitness) {
        bestParams = ParameterArrayWithFitness(sample, fitness)
        notifyOfChange(bestParams)
      }
      if (isOptimalFitnessReached(bestParams))
        done = true
    }
    bestParams
  }
}

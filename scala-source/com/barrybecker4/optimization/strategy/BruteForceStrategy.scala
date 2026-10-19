// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
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
        enumerateIntegerLattice(numeric)
      case _ =>
        searchSamples(params)
    }

  private def hasContinuousParameter(params: NumericParameterArray): Boolean =
    (0 until params.size).exists(i => !params.get(i).isIntegerOnly)

  /** Every inclusive integer combination. Stops early when the known optimum is reached. */
  private def enumerateIntegerLattice(template: NumericParameterArray): ParameterArrayWithFitness = {
    val bounds = integerBounds(template)
    requireEnumerable(template, bounds)
    new LatticeSearch(template, bounds).run()
  }

  private def integerBounds(template: NumericParameterArray): IndexedSeq[(Int, Int)] =
    (0 until template.size).map { i =>
      val p = template.get(i)
      (p.minValue.round.toInt, p.maxValue.round.toInt)
    }

  /** Fails fast, before any evaluation, if a range is empty or the lattice is too big to finish. */
  private def requireEnumerable(template: NumericParameterArray, bounds: IndexedSeq[(Int, Int)]): Unit = {
    var product = 1L
    bounds.zipWithIndex.foreach { case ((lo, hi), i) =>
      val span = hi.toLong - lo + 1
      require(span > 0, s"BRUTE_FORCE parameter ${template.get(i).name} has an empty integer range [$lo, $hi]")
      require(product <= MaxIntegerCandidates / span,
        s"BRUTE_FORCE integer space has more than $MaxIntegerCandidates candidates")
      product *= span
    }
  }

  /** Depth first enumeration of the lattice, one dimension per level of recursion. */
  private final class LatticeSearch(template: NumericParameterArray, bounds: IndexedSeq[(Int, Int)]) {

    private val coords = Array.fill(bounds.size)(0.0)
    private var best = ParameterArrayWithFitness(template, Double.MaxValue)

    def run(): ParameterArrayWithFitness = {
      enumerate(0)
      best
    }

    private def done: Boolean = isOptimalFitnessReached(best)

    private def enumerate(dim: Int): Unit =
      if (dim == bounds.size) consider()
      else {
        val (lo, hi) = bounds(dim)
        var v = lo
        while (v <= hi && !done) {
          coords(dim) = v.toDouble
          enumerate(dim + 1)
          v += 1
        }
      }

    private def consider(): Unit = {
      val candidate = NumericParameterArray(
        coords.indices.map(i => template.get(i).setValue(coords(i))),
        template.numSteps,
        template.rnd
      )
      val fitness = scoreAgainst(candidate, template)
      if (fitness < best.fitness) {
        best = ParameterArrayWithFitness(candidate, fitness)
        notifyOfChange(best)
      }
    }
  }

  private def searchSamples(params: ParameterArray): ParameterArrayWithFitness = {
    val samples = params.findGlobalSamples(Long.MaxValue)
    var best = ParameterArrayWithFitness(params, Double.MaxValue)
    while (samples.hasNext && !isOptimalFitnessReached(best)) {
      val sample = samples.next()
      val fitness = scoreAgainst(sample, params)
      if (fitness < best.fitness) {
        best = ParameterArrayWithFitness(sample, fitness)
        notifyOfChange(best)
      }
    }
    best
  }
}

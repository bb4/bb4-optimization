// Copyright by Barry G. Becker, 2001-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.CollectionConverters.*
import scala.util.{Failure, Random, Success, Try}

/**
  * Concurrent (parallelized) Genetic Algorithm optimization strategy.
  *
  * Parallelizes:
  *  - compare-mode population re-evaluation vs a generation baseline
  *  - batch offspring fitness evaluation ([[evaluateOffspring]]) for initial seeding and refill
  *
  * Parent selection, crossover, and neighbor proposal stay on the calling thread. With the default
  * [[NeighborSelection.GreedyImproveAbsolute]] each mutation is a short sequential chain of retries, so it is not
  * parallelized; choose [[NeighborSelection.SingleSample]] to get parallel speed-up for absolute fitness.
  *
  * '''Thread safety:''' the [[com.barrybecker4.optimization.optimizee.Optimizee]] must be safe for concurrent `evaluateFitness` /
  * `compareFitness` calls (typical for immutable puzzle/sim objectives). If the optimizee
  * mutates shared state, use [[GeneticSearchStrategy]] instead or synchronize externally.
  *
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class ConcurrentGeneticSearchStrategy(
    optimizee: Optimizee,
    rnd: Random = MathUtil.RANDOM,
    config: GeneticSearchConfig = GeneticSearchConfig.default
) extends GeneticSearchStrategy(optimizee, rnd, config) {

  /**
    * Parallel re-evaluation of comparison-mode fitness for each member vs `baseline`.
    */
  override protected def reevaluatePopulationFitness(population: ArrayBuffer[ParameterArrayWithFitness],
                                                     baseline: ParameterArray): Unit =
    if (optimizee.evaluateByComparison) {
      val updated = population.toVector.par.map { m =>
        ParameterArrayWithFitness(m.pa, optimizee.compareFitness(m.pa, baseline))
      }.seq
      updated.indices.foreach(i => population(i) = updated(i))
    }

  /**
    * Parallel fitness evaluation of offspring; results are returned in the same order as `candidates`
    * so merge order stays deterministic for a fixed proposal sequence.
    *
    * Every candidate is attempted even if another one fails (for example when the evaluation budget runs out), so
    * the ones that did succeed are still recorded before the first failure is rethrown.
    */
  override protected def evaluateOffspring(
      candidates: Seq[ParameterArray],
      generationBaseline: ParameterArray
  ): Seq[ParameterArrayWithFitness] = {
    val outcomes = candidates.toVector.par.map(pa => Try(evaluateParameterArray(pa, generationBaseline))).seq
    val scored = outcomes.collect { case Success(m) => m }
    outcomes.collectFirst { case Failure(e) => e }.foreach { failure =>
      recordPartialResults(scored)
      throw failure
    }
    scored
  }
}

// Copyright by Barry G. Becker, 2001-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.CollectionConverters.*
import scala.util.Random

/**
  * Concurrent (parallelized) Genetic Algorithm optimization strategy.
  *
  * Parallelizes:
  *  - compare-mode population re-evaluation vs a generation baseline
  *  - batch offspring fitness evaluation ([[evaluateOffspring]]) for initial seeding and refill
  *
  * Parent selection, crossover, and neighbor proposal stay on the calling thread.
  *
  * '''Thread safety:''' the [[Optimizee]] must be safe for concurrent `evaluateFitness` /
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
                                                     baseline: ParameterArray): Unit = {
    if (!optimizee.evaluateByComparison) return
    val updated = population.indices.par.map { i =>
      val p = population(i).pa
      ParameterArrayWithFitness(p, optimizee.compareFitness(p, baseline))
    }.toIndexedSeq
    var i = 0
    while (i < population.size) {
      population(i) = updated(i)
      i += 1
    }
  }

  /**
    * Parallel fitness evaluation of offspring; results are returned in the same order as `candidates`
    * so merge order stays deterministic for a fixed proposal sequence.
    */
  override protected def evaluateOffspring(
      candidates: Seq[ParameterArray],
      generationBaseline: ParameterArray
  ): Seq[ParameterArrayWithFitness] = {
    if (candidates.isEmpty) return IndexedSeq.empty
    candidates.indices.par.map { i =>
      i -> evaluateParameterArray(candidates(i), generationBaseline)
    }.toIndexedSeq.sortBy(_._1).map(_._2)
  }
}

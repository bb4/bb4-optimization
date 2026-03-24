// Copyright by Barry G. Becker, 2001-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.collection.parallel.CollectionConverters._

/**
  * Concurrent (i.e. parallelized) Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class ConcurrentGeneticSearchStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends GeneticSearchStrategy(optimizee, rnd) {

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
}

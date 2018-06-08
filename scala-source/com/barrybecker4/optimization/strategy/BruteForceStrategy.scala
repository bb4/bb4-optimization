// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray

/**
  * A strategy which naively tries all possibilities.
  * This will not be practical for problems with real valued parameters where the search space is infinite.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class BruteForceStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** Systematically search the entire global space and return the best of the samples.
    * Stops if the optimal fitness is reached.
    * @param params       the params to compare evaluation against if we evaluate BY_COMPARISON.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return best solution found using global sampling.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    val samples = params.findGlobalSamples(Long.MaxValue)
    var bestFitness = Double.MaxValue
    var bestParams = params.copy
    var done = false
    while (samples.hasNext && !done) {
      val sample = samples.next
      var fitness = .0
      if (optimizee.evaluateByComparison) fitness = optimizee.compareFitness(sample, params)
      else fitness = optimizee.evaluateFitness(sample)
      sample.setFitness(fitness)
      if (fitness < bestFitness) {
        bestFitness = fitness
        notifyOfChange(sample)
        bestParams = sample.copy
      }
      if (isOptimalFitnessReached(bestParams))
        done = true
    }
    bestParams
  }
}
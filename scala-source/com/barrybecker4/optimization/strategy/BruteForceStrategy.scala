// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

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
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
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
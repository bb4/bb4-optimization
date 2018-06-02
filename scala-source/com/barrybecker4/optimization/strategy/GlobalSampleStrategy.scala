package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray


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

  numSample = GlobalSampleStrategy.DEFAULT_NUM_SAMPLES

  /** the user should set this explicitly. */
  private var numSample = 0

  /**
    * @param samplingRate the rate at which to sample along each dimension when trying guesses globally.
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
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    val samples = params.findGlobalSamples(numSample)
    var bestFitness = Double.MaxValue
    var bestParams = params.copy
    var done = false

    while (samples.hasNext && !done) {
      val sample = samples.next
      var fitness = .0
      if (optimizee.evaluateByComparison) fitness = optimizee.compareFitness(sample, params)
      else fitness = optimizee.evaluateFitness(sample)
      sample.setFitness(fitness)
      //System.out.println( "key = " + hashKey + '\n' + testParams + "\n  fitness=" + fitness );
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
// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.ParameterArray


object GlobalHillClimbingStrategy {
  private val NUM_SAMPLES = 1000
}

/**
  * This is a hybrid optimization strategy.
  * Use a hardcoded static data interface to initialize.
  * so it can be easily run in an applet without using resources.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class GlobalHillClimbingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /**
    * Perform the optimization of the optimizee.
    * @param params parameter array
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return optimized params
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    val sampleResult = doSampleOptimization(params, fitnessRange)
    doHillClimbingOptimization(sampleResult, fitnessRange)
  }

  private def doSampleOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    val gsStrategy = new GlobalSampleStrategy(optimizee)
    gsStrategy.setListener(listener)
    // 3 sample points along each dimension
    gsStrategy.setSamplingRate(GlobalHillClimbingStrategy.NUM_SAMPLES)
    // first find a good place to start
    // perhaps we should try several of the better results from global sampling.
    gsStrategy.doOptimization(params, fitnessRange)
  }

  private def doHillClimbingOptimization(params: ParameterArray, fitnessRange: Double): ParameterArray = {
    val strategy = new HillClimbingStrategy(optimizee)
    strategy.setListener(listener)
    strategy.doOptimization(params, fitnessRange)
  }
}
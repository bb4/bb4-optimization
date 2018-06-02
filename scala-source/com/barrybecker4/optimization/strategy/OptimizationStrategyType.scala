// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee


/**
  * Enum for the different possible Optimization Strategies.
  * There is an optimization strategy class corresponding to each of these types.
  * Detailed explanations for many of these algorithms can be found in
  * How To Solve It: Modern Heuristics  by Michaelwics and Fogel
  * @author Barry Becker
  */
sealed trait OptimizationStrategyType {

  def getDescription: String

  /**
    * Create an instance of the strategy to use.
    * @param optimizee    the thing to optimize.
    * @param fitnessRange the approximate range (max-min) of the fitness values
    * @return an instance of the strategy to use.
    */
  def getStrategy(optimizee: Optimizee, fitnessRange: Double): OptimizationStrategy
}


case object GLOBAL_SAMPLING extends OptimizationStrategyType {

  override def getDescription: String = "Sparsely sample the space and return the best sample."

  override def getStrategy(optimizee: Optimizee, fitnessRange: Double): OptimizationStrategy = {
    val gsStrategy = new GlobalSampleStrategy(optimizee)
    gsStrategy.setSamplingRate(1000)
    gsStrategy
  }
}

case object SIMULATED_ANNEALING extends OptimizationStrategyType {

  override def getDescription: String = "Marches in the general direction of improvement, but can escape local optima."

  override def getStrategy(optimizee: Optimizee, fitnessRange: Double): OptimizationStrategy = {
    val strategy = new SimulatedAnnealingStrategy(optimizee)
    strategy.setMaxTemperature(fitnessRange / 10.0)
    strategy
  }
}
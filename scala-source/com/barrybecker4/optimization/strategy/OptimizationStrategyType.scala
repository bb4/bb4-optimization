// Copyright by Barry G. Becker, 2000-2023. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}

import scala.util.Random


object OptimizationStrategyType {
   val VALUES: Array[OptimizationStrategyType[? <: ParameterArray]] = Array(
     GLOBAL_SAMPLING, GLOBAL_HILL_CLIMBING, HILL_CLIMBING, SIMULATED_ANNEALING, GENETIC_SEARCH, BRUTE_FORCE
   )
}

/**
  * Enum for the different possible Optimization Strategies.
  * There is an optimization strategy class corresponding to each of these types.
  * Detailed explanations for many of these algorithms can be found in
  * How To Solve It: Modern Heuristics by Michaelwicz and Fogel
  * @author Barry Becker
  */
sealed trait OptimizationStrategyType[P <: ParameterArray] {

  def getDescription: String

  /**
    * Create an instance of the strategy to use.
    * @param optimizee    the thing to optimize.
    * @param fitnessRange the approximate range (max-min) of the fitness values
    * @return an instance of the strategy to use.
    */
  def getStrategy(optimizee: Optimizee[P], fitnessRange: Double, rnd: Random): OptimizationStrategy[P]
}


case object GLOBAL_SAMPLING extends OptimizationStrategyType[NumericParameterArray] {
  override def getDescription: String = "Sparsely sample the space and return the best sample."

  override def getStrategy(optimizee: Optimizee[NumericParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[NumericParameterArray] = {
    val gsStrategy = new GlobalSampleStrategy(optimizee)
    gsStrategy.setSamplingRate(1000)
    gsStrategy
  }
}

case object GLOBAL_HILL_CLIMBING extends OptimizationStrategyType[NumericParameterArray] {
  override def getDescription: String = "Start with the best global sampling and hill climb from there."

  override def getStrategy(optimizee: Optimizee[NumericParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[NumericParameterArray] =
    new GlobalHillClimbingStrategy(optimizee)
}

case object HILL_CLIMBING extends OptimizationStrategyType[NumericParameterArray] {
  override def getDescription: String =
    "Search method which always marches toward the direction of greatest improvement."

  override def getStrategy(optimizee: Optimizee[NumericParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[NumericParameterArray] =
    new HillClimbingStrategy(optimizee)
}

case object SIMULATED_ANNEALING extends OptimizationStrategyType[ParameterArray] {
  override def getDescription: String =
    "Marches in the general direction of improvement, but can escape local optima."

  override def getStrategy(optimizee: Optimizee[ParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[ParameterArray] = {
    val strategy = new SimulatedAnnealingStrategy(optimizee, rnd)
    strategy.setMaxTemperature(fitnessRange / 10.0)
    strategy
  }
}


case object GENETIC_SEARCH extends OptimizationStrategyType[ParameterArray] {
  override def getDescription: String =
    "Uses a genetic algorithm to search for the best solution."

  override def getStrategy(optimizee: Optimizee[ParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[ParameterArray] = {
    val strategy = new GeneticSearchStrategy(optimizee, rnd)
    strategy.setImprovementEpsilon(fitnessRange / 100000000.0)
    strategy
  }
}

case object CONCURRENT_GENETIC_SEARCH extends OptimizationStrategyType[ParameterArray] {
  override def getDescription: String =
    "Uses a concurrent genetic algorithm to search for the best solution."

  override def getStrategy(optimizee: Optimizee[ParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[ParameterArray] = {
    val strategy = new ConcurrentGeneticSearchStrategy(optimizee, rnd)
    strategy.setImprovementEpsilon(fitnessRange / 100000000.0)
    strategy
  }
}

case object STATE_SPACE_SEARCH extends OptimizationStrategyType[ParameterArray] {
  override def getDescription: String =
    "Searches the state space to find an optima."

  override def getStrategy(optimizee: Optimizee[ParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[ParameterArray] =
    throw new AbstractMethodError("State space search not yet implemented")
}

case object BRUTE_FORCE extends OptimizationStrategyType[ParameterArray] {
  override def getDescription: String =
    "Tries all possible combinations in order to find the best possible. " +
      "Not possible if parameter space has real values."

  override def getStrategy(optimizee: Optimizee[ParameterArray], fitnessRange: Double, rnd: Random): OptimizationStrategy[ParameterArray] =
    new BruteForceStrategy(optimizee)
}
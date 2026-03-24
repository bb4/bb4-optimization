// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee

import scala.util.Random

/**
  * Enum for the different possible Optimization Strategies.
  * There is an optimization strategy class corresponding to each of these types.
  * Detailed explanations for many of these algorithms can be found in
  * How To Solve It: Modern Heuristics by Michaelwicz and Fogel
  * @author Barry Becker
  */
enum OptimizationStrategyType {

  case GLOBAL_SAMPLING,
       GLOBAL_HILL_CLIMBING,
       HILL_CLIMBING,
       SIMULATED_ANNEALING,
       GENETIC_SEARCH,
       CONCURRENT_GENETIC_SEARCH,
       STATE_SPACE_SEARCH,
       BRUTE_FORCE

  def getDescription: String = this match
    case GLOBAL_SAMPLING =>
      "Sparsely sample the space and return the best sample."
    case GLOBAL_HILL_CLIMBING =>
      "Start with the best global sampling and hill climb from there."
    case HILL_CLIMBING =>
      "Search method which always marches toward the direction of greatest improvement."
    case SIMULATED_ANNEALING =>
      "Marches in the general direction of improvement, but can escape local optima."
    case GENETIC_SEARCH =>
      "Uses a genetic algorithm to search for the best solution."
    case CONCURRENT_GENETIC_SEARCH =>
      "Uses a concurrent genetic algorithm to search for the best solution."
    case STATE_SPACE_SEARCH =>
      "Searches the state space to find an optima."
    case BRUTE_FORCE =>
      "Tries all possible combinations in order to find the best possible. " +
        "Not possible if parameter space has real values."

  def getStrategy(optimizee: Optimizee, fitnessRange: Double, rnd: Random): OptimizationStrategy =
    this match
      case GLOBAL_SAMPLING =>
        val gsStrategy = new GlobalSampleStrategy(optimizee)
        gsStrategy.setSamplingRate(1000)
        gsStrategy
      case GLOBAL_HILL_CLIMBING =>
        new GlobalHillClimbingStrategy(optimizee)
      case HILL_CLIMBING =>
        new HillClimbingStrategy(optimizee)
      case SIMULATED_ANNEALING =>
        val strategy = new SimulatedAnnealingStrategy(optimizee, rnd)
        strategy.setMaxTemperature(fitnessRange / 10.0)
        strategy
      case GENETIC_SEARCH =>
        val strategy = new GeneticSearchStrategy(optimizee, rnd)
        strategy.setImprovementEpsilon(fitnessRange / 100000000.0)
        strategy
      case CONCURRENT_GENETIC_SEARCH =>
        val strategy = new ConcurrentGeneticSearchStrategy(optimizee, rnd)
        strategy.setImprovementEpsilon(fitnessRange / 100000000.0)
        strategy
      case STATE_SPACE_SEARCH =>
        throw new AbstractMethodError("State space search not yet implemented")
      case BRUTE_FORCE =>
        new BruteForceStrategy(optimizee)
}

object OptimizationStrategyType {

  /** Strategies shown in UI dropdowns (excludes unimplemented state-space search). */
  val valuesForUi: Array[OptimizationStrategyType] = Array(
    GLOBAL_SAMPLING,
    GLOBAL_HILL_CLIMBING,
    HILL_CLIMBING,
    SIMULATED_ANNEALING,
    GENETIC_SEARCH,
    CONCURRENT_GENETIC_SEARCH,
    BRUTE_FORCE
  )

  /** @deprecated use [[valuesForUi]] */
  @deprecated("use valuesForUi", since = "2.0")
  val VALUES: Array[OptimizationStrategyType] = valuesForUi
}

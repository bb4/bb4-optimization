// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{DiscreteStateSpace, Optimizee}
import com.barrybecker4.optimization.parameter.{
  NumericParameterArray, ParameterArray, PermutedParameterArray, VariableLengthIntSet
}

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
       GLOBAL_SIMULATED_ANNEALING,
       HILL_CLIMBING,
       SIMULATED_ANNEALING,
       GENETIC_SEARCH,
       CONCURRENT_GENETIC_SEARCH,
       STATE_SPACE_SEARCH,
       BRUTE_FORCE,
       DIFFERENTIAL_EVOLUTION,
       PARTICLE_SWARM,
       ITERATED_LOCAL_SEARCH,
       VARIABLE_NEIGHBORHOOD_SEARCH

  def getDescription: String = this match
    case GLOBAL_SAMPLING =>
      "Sparsely sample the space and return the best sample."
    case GLOBAL_HILL_CLIMBING =>
      "Global sampling, then hill climb from each of the best few samples; return the best local optimum."
    case GLOBAL_SIMULATED_ANNEALING =>
      "Global sampling, then simulated annealing from each of the best few samples; return the best result."
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
    case DIFFERENTIAL_EVOLUTION =>
      "Excellent default for continuous, bounded, non-linear parameter optimization. " +
        "Employs population vector differences for mutation, followed by binomial crossover and one-to-one selection."
    case PARTICLE_SWARM =>
      "A swarm of particles flies through the bounded parameter space. Works for continuous and discrete problems."
    case ITERATED_LOCAL_SEARCH => 
      "Iterated local search (ILS) for discrete problems such as permutations."
    case VARIABLE_NEIGHBORHOOD_SEARCH =>
      "Variable neighborhood search (VNS) for discrete problems such as permutations."

  /**
    * True when this strategy can run on `optimizee` with `params`.
    * False only for combinations that the strategy rejects up front (wrong parameter
    * shape, continuous enumeration, or comparison fitness). A poor fit that still runs,
    * such as hill climbing on a multimodal surface, stays true.
    */
  def appliesTo(optimizee: Optimizee, params: ParameterArray): Boolean =
    this match
      case STATE_SPACE_SEARCH =>
        optimizee.isInstanceOf[DiscreteStateSpace]
      case BRUTE_FORCE =>
        !hasContinuousParameter(params)
      case DIFFERENTIAL_EVOLUTION | PARTICLE_SWARM =>
        !optimizee.evaluateByComparison && params.isInstanceOf[NumericParameterArray]
      case HILL_CLIMBING | GLOBAL_HILL_CLIMBING =>
        locallySearchable(params)
      case _ =>
        true

  def getStrategy(optimizee: Optimizee, fitnessRange: Double, rnd: Random): OptimizationStrategy =
    this match
      case GLOBAL_SAMPLING =>
        val gsStrategy = new GlobalSampleStrategy(optimizee)
        gsStrategy.setSamplingRate(1000)
        gsStrategy
      case GLOBAL_HILL_CLIMBING =>
        new GlobalHillClimbingStrategy(optimizee)
      case GLOBAL_SIMULATED_ANNEALING =>
        new GlobalSimulatedAnnealingStrategy(optimizee, rnd)
      case HILL_CLIMBING =>
        new HillClimbingStrategy(optimizee)
      case SIMULATED_ANNEALING =>
        val strategy = new SimulatedAnnealingStrategy(optimizee, rnd)
        strategy.setConfig(SimulatedAnnealingConfig())
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
        val strategy = new StateSpaceSearchStrategy(optimizee)
        strategy.setConfig(StateSpaceSearchConfig.default)
        strategy
      case BRUTE_FORCE =>
        new BruteForceStrategy(optimizee)
      case DIFFERENTIAL_EVOLUTION =>
        new DifferentialEvolutionStrategy(optimizee, rnd = rnd)
      case PARTICLE_SWARM =>
        new ParticleSwarmStrategy(optimizee, rnd)
      case ITERATED_LOCAL_SEARCH =>
        IteratedLocalSearchStrategy.strategyFor(optimizee, IteratedLocalSearchConfig.IteratedLocalSearch, rnd)
      case VARIABLE_NEIGHBORHOOD_SEARCH =>
        IteratedLocalSearchStrategy.strategyFor(optimizee, IteratedLocalSearchConfig.VariableNeighborhoodSearch, rnd)

  private def hasContinuousParameter(params: ParameterArray): Boolean =
    params match
      case numeric: NumericParameterArray =>
        (0 until numeric.size).exists(i => !numeric.get(i).isIntegerOnly)
      case _ =>
        false

  private def locallySearchable(params: ParameterArray): Boolean =
    params match
      case _: NumericParameterArray | _: PermutedParameterArray | _: VariableLengthIntSet => true
      case _ => false
}

object OptimizationStrategyType {

  /** Catalog offered by UI dropdowns. Each problem keeps the entries for which [[OptimizationStrategyType.appliesTo]] is true. */
  val valuesForUi: Array[OptimizationStrategyType] = Array(
    GLOBAL_SAMPLING,
    GLOBAL_HILL_CLIMBING,
    GLOBAL_SIMULATED_ANNEALING,
    HILL_CLIMBING,
    SIMULATED_ANNEALING,
    GENETIC_SEARCH,
    CONCURRENT_GENETIC_SEARCH,
    STATE_SPACE_SEARCH,
    BRUTE_FORCE,
    DIFFERENTIAL_EVOLUTION,
    PARTICLE_SWARM,
    ITERATED_LOCAL_SEARCH, 
    VARIABLE_NEIGHBORHOOD_SEARCH
  )

  /** @deprecated use [[valuesForUi]] */
  @deprecated("use valuesForUi", since = "2.0")
  val VALUES: Array[OptimizationStrategyType] = valuesForUi
}

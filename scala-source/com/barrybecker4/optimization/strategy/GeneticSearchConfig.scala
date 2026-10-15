// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

/**
  * How absolute-fitness neighbor mutation picks a candidate.
  * Compare-mode always takes a single sample vs the generation baseline.
  */
enum NeighborSelection {

  /** One random neighbor, one evaluation (absolute fitness). */
  case SingleSample

  /**
    * Absolute fitness: try up to `maxNeighborRetries` neighbors until one beats the parent
    * (legacy greedy behavior). Compare-mode ignores this and uses a single compare.
    */
  case GreedyImproveAbsolute

  /** Alias for compare-mode single-sample path; same as [[SingleSample]] for absolute fitness. */
  case AcceptFirst
}

/**
  * Tunables for [[GeneticSearchStrategy]] (defaults match prior hardcoded constants).
  *
  * @param crossoverEnabled       when false, refill uses neighbor mutation only
  * @param cullFactor             fraction of population discarded each generation (0–1)
  * @param crossoverProbability   chance of crossover vs mutation when filling after cull
  * @param maxIterations          hard cap on generations
  * @param maxStagnantGenerations stop after this many generations with negligible improvement
  * @param initialNeighborRadius  radius for seeding the initial population
  * @param nbrRadius              starting mutation neighborhood radius during evolve
  * @param nbrRadiusShrinkFactor  multiply radius when improvement is small
  * @param nbrRadiusExpandFactor  multiply radius when improvement is large
  * @param nbrRadiusSoftener      softens radius scaling by keeper rank
  * @param maxNeighborRetries     retries for [[NeighborSelection.GreedyImproveAbsolute]]
  * @param neighborSelection      absolute-fitness neighbor policy
  * @param improvementEpsilon     stagnation threshold; factory may override from fitnessRange
  * @param elitismEnabled         reinject global champion after cull/refill
  */
case class GeneticSearchConfig(
    crossoverEnabled: Boolean = true,
    cullFactor: Double = 0.7,
    crossoverProbability: Double = 0.65,
    maxIterations: Int = 100,
    maxStagnantGenerations: Int = 3,
    initialNeighborRadius: Double = 1.0,
    nbrRadius: Double = 0.1,
    nbrRadiusShrinkFactor: Double = 0.8,
    nbrRadiusExpandFactor: Double = 1.1,
    nbrRadiusSoftener: Double = 20.0,
    maxNeighborRetries: Int = 8,
    neighborSelection: NeighborSelection = NeighborSelection.GreedyImproveAbsolute,
    improvementEpsilon: Double = GeneticSearchConfig.DEFAULT_IMPROVEMENT_EPS,
    elitismEnabled: Boolean = true
)

object GeneticSearchConfig {
  /** Default stagnation epsilon (factory may override from fitnessRange). */
  val DEFAULT_IMPROVEMENT_EPS: Double = 0.000000000001

  val default: GeneticSearchConfig = GeneticSearchConfig()
}

/** @deprecated use [[GeneticSearchConfig]] */
@deprecated("use GeneticSearchConfig", since = "2.1")
object GeneticConfig {
  def apply(crossoverEnabled: Boolean = true): GeneticSearchConfig =
    GeneticSearchConfig(crossoverEnabled = crossoverEnabled)
}

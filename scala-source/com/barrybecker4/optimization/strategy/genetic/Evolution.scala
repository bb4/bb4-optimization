// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness.given
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStrategy

import scala.collection.mutable.ArrayBuffer

/** What happened in one generation. */
private[genetic] final case class GenerationReport(
    index: Int,
    champion: ParameterArrayWithFitness,
    previousChampion: ParameterArrayWithFitness,
    /** Change in the champion's fitness: 0 (no progress) or negative (progress). */
    delta: Double,
    /** The base mutation radius used during the generation. */
    radius: Double
)

/**
  * The generational loop of a genetic search: each generation re-scores, culls and refills the population, then
  * records any new champion. Decides when to stop (optimum reached, stagnation, or generation limit) and adapts the
  * mutation radius to the rate of progress.
  *
  * @param rescore refreshes the fitness of every member against a baseline (comparison mode only does anything)
  */
private[genetic] final class Evolution(
    initialPopulation: ArrayBuffer[ParameterArrayWithFitness],
    champions: ChampionTracker,
    refiller: PopulationRefiller,
    rescore: (ArrayBuffer[ParameterArrayWithFitness], ParameterArray) => Unit,
    config: GeneticSearchConfig,
    desiredSize: Int,
    fitnessRange: Double
) {
  private var population = initialPopulation
  private var generation = 0
  private var delta = -1.0
  private var stagnantGenerations = 0
  private var radius = config.nbrRadius
  private val lowThresh = 0.005 * fitnessRange
  private val highThresh = 0.05 * fitnessRange

  /** @param optimalReached whether the champion already has the optimal fitness */
  def stopReason(optimalReached: Boolean): Option[String] =
    if (optimalReached) Some("we found the optimal fitness.")
    else if (isStagnant && stagnantGenerations > config.maxStagnantGenerations)
      Some(s"we made no IMPROVEMENT. The delta, $delta was >= ${-config.improvementEpsilon}")
    else if (generation >= config.maxIterations)
      Some(s"we reached the MAX ITERATIONS(${config.maxIterations}): num iterations = $generation")
    else None

  /** Number of generations completed. */
  def generations: Int = generation

  def nextGeneration(): GenerationReport = {
    val before = champions.get.getOrElse(population.min)
    champions.beginGeneration()
    rescore(population, before.pa)
    population = cull(population)
    refiller.refill(population, population.size, desiredSize, before.pa, radius)
    if (config.elitismEnabled) champions.ensureInPopulation(population, desiredSize)
    champions.considerMember(population.min)
    updateProgress(before)
  }

  /** Remove all but the best candidates. Better candidates have lower values. */
  private def cull(members: ArrayBuffer[ParameterArrayWithFitness]): ArrayBuffer[ParameterArrayWithFitness] = {
    val keepSize = math.max(1, (members.size * (1.0 - config.cullFactor)).toInt)
    members.sorted.take(keepSize)
  }

  private def isStagnant: Boolean = delta >= -config.improvementEpsilon

  /** Records the generation's progress and adapts the radius for the next one. */
  private def updateProgress(before: ParameterArrayWithFitness): GenerationReport = {
    val after = champions.orElse(before)
    delta = math.min(after.fitness - before.fitness, 0.0)
    val report = GenerationReport(generation, after, before, delta, radius)
    radius = scaledRadius()
    generation += 1
    if (isStagnant) stagnantGenerations += 1
    report
  }

  /** Big improvements widen the search; little or none narrows it. */
  private def scaledRadius(): Double = {
    val factor =
      if (delta < -highThresh) config.nbrRadiusExpandFactor
      else if (delta > -lowThresh) config.nbrRadiusShrinkFactor
      else 1.0
    math.min(SimulatedAnnealingStrategy.MAX_PROPOSAL_RADIUS, radius * factor)
  }
}

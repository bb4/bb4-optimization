// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable.ArrayBuffer

/**
  * Remembers the best individual found during a run and keeps its fitness comparable across generations.
  *
  * In absolute mode a member's fitness is its objective value and needs no translation.
  *
  * In comparison mode members are scored against a baseline (the champion when the generation started), so
  * their fitness only means "improvement over that baseline". The champion's fitness is instead the ''cumulative''
  * improvement over the initial guess, and members are translated with [[toChampionFrame]] before being compared
  * with it, and back with [[toPopulationFrame]] when the champion is put back into a population.
  *
  * @param compareMode  whether the optimizee scores candidates by comparison
  * @param onCandidate  told about every candidate considered, whether or not it becomes the champion
  */
private[genetic] final class ChampionTracker(compareMode: Boolean,
                                             onCandidate: ParameterArrayWithFitness => Unit) {

  private var current: Option[ParameterArrayWithFitness] = None

  /** The champion's cumulative fitness when the current generation started (0 while seeding). */
  private var baselineFitness = 0.0

  def reset(): Unit = {
    current = None
    baselineFitness = 0.0
  }

  def get: Option[ParameterArrayWithFitness] = current

  /** The champion, or `fallback` if nothing has been considered yet. */
  def orElse(fallback: ParameterArrayWithFitness): ParameterArrayWithFitness = current.getOrElse(fallback)

  /** Call when a generation starts: its members will be scored against the current champion. */
  def beginGeneration(): Unit = baselineFitness = current.fold(0.0)(_.fitness)

  /** @param candidate fitness already in the champion's frame */
  def consider(candidate: ParameterArrayWithFitness): Unit = {
    onCandidate(candidate)
    if (current.forall(_.fitness > candidate.fitness)) current = Some(candidate)
  }

  /** @param member fitness scored against the current generation's baseline */
  def considerMember(member: ParameterArrayWithFitness): Unit = consider(toChampionFrame(member))

  def toChampionFrame(member: ParameterArrayWithFitness): ParameterArrayWithFitness =
    if (compareMode) member.copy(fitness = baselineFitness + member.fitness) else member

  def toPopulationFrame(champion: ParameterArrayWithFitness): ParameterArrayWithFitness =
    if (compareMode) champion.copy(fitness = champion.fitness - baselineFitness) else champion

  /** Elitism: reinject the champion if culling removed it, replacing the worst member when the population is full. */
  def ensureInPopulation(population: ArrayBuffer[ParameterArrayWithFitness], desiredSize: Int): Unit =
    current.foreach { champ =>
      if (!Solutions.contains(population, champ.pa)) {
        val member = toPopulationFrame(champ)
        if (population.size < desiredSize) population += member
        else population(population.indices.maxBy(i => population(i).fitness)) = member
      }
    }

  /** Test hook: pretend `champion` was found. */
  def set(champion: ParameterArrayWithFitness): Unit = {
    current = Some(champion)
    onCandidate(champion)
  }
}

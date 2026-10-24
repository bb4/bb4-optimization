// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStrategy

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  * Refills a culled population with crossover children and mutants of the keepers.
  * Crossover and single-sample mutation proposals are batched through `evaluate`; greedy absolute mutation
  * evaluates inline.
  *
  * @param evaluate scores a batch of candidates against a baseline (may be parallel)
  * @param onAdded  told about each member added to the population
  */
private[genetic] final class PopulationRefiller(
    optimizee: Optimizee,
    config: GeneticSearchConfig,
    rnd: Random,
    evaluate: (Seq[ParameterArray], ParameterArray) => Seq[ParameterArrayWithFitness],
    onAdded: ParameterArrayWithFitness => Unit
) {

  /**
    * Add members to `population` until it has `desiredSize` (or the attempt limit is hit).
    *
    * @param keepSize the first `keepSize` members are the keepers that parents are drawn from
    * @param baseline what candidates are scored against in comparison mode
    * @param nbrRadius the current base mutation radius
    */
  def refill(population: ArrayBuffer[ParameterArrayWithFitness],
             keepSize: Int,
             desiredSize: Int,
             baseline: ParameterArray,
             nbrRadius: Double): Unit =
    new Pass(population, keepSize, desiredSize, baseline, nbrRadius).run()

  /** Mutation radius grows with the rank of the parent (worse parents take bigger steps). */
  private def mutationRadius(keeperIndex: Int, nbrRadius: Double): Double = {
    val softener = config.nbrRadiusSoftener
    val raw = (keeperIndex + softener) / softener * nbrRadius
    math.min(SimulatedAnnealingStrategy.MAX_PROPOSAL_RADIUS, raw)
  }

  private val useGreedy =
    !optimizee.evaluateByComparison && config.neighborSelection == NeighborSelection.GreedyImproveAbsolute

  /** One refill: the proposals not yet evaluated, and everything seen so far so duplicates are cheap to detect. */
  private final class Pass(population: ArrayBuffer[ParameterArrayWithFitness],
                           keepSize: Int,
                           desiredSize: Int,
                           baseline: ParameterArray,
                           nbrRadius: Double) {
    private val pending = ArrayBuffer[ParameterArray]()
    private val seen = mutable.HashSet.from(population.map(m => Solutions.key(m.pa)))

    def run(): Unit = {
      val maxAttempts = math.max(100 * desiredSize, (desiredSize - keepSize) * 50 + 50)
      var attempts = 0
      while (population.size + pending.size < desiredSize && attempts < maxAttempts) {
        attempts += 1
        if (!queueCrossoverChild()) mutateKeeper()
      }
      flushPending()
    }

    private def shouldCross: Boolean =
      config.crossoverEnabled && rnd.nextDouble() < config.crossoverProbability && keepSize >= 2

    /** @return true if a new child was queued for evaluation */
    private def queueCrossoverChild(): Boolean =
      shouldCross && {
        val (i1, i2) = pickTwoParents()
        val p1 = population(i1).pa
        val p2 = population(i2).pa
        p1.getClass == p2.getClass && crossIntoPending(p1, p2)
      }

    /** Two distinct keeper indexes, biased toward the best. Requires `keepSize >= 2`. */
    private def pickTwoParents(): (Int, Int) = {
      val i1 = biasedKeeperIndex()
      var i2 = biasedKeeperIndex()
      while (i2 == i1) i2 = (i2 + 1) % keepSize
      (i1, i2)
    }

    private def biasedKeeperIndex(): Int = (rnd.nextDouble() * rnd.nextDouble() * keepSize).toInt

    private def crossIntoPending(p1: ParameterArray, p2: ParameterArray): Boolean =
      try queueIfNew(p1.crossWith(p2, rnd))
      catch {
        case _: IllegalArgumentException => false // parents that cannot be crossed
      }

    private def queueIfNew(candidate: ParameterArray): Boolean = {
      val isNew = seen.add(Solutions.key(candidate))
      if (isNew) pending += candidate
      isNew
    }

    private def mutateKeeper(): Unit = {
      val r = rnd.nextDouble()
      val keeperIndex = (r * r * keepSize).toInt
      val parent = population(keeperIndex)
      val radius = mutationRadius(keeperIndex, nbrRadius)
      if (useGreedy) addGreedyMutant(parent, radius)
      else queueIfNew(parent.pa.getRandomNeighbor(radius))
    }

    private def addGreedyMutant(parent: ParameterArrayWithFitness, radius: Double): Unit = {
      flushPending()
      val mutant = greedyNeighbor(parent, radius)
      if (seen.add(Solutions.key(mutant.pa))) addToPopulation(mutant)
    }

    private def flushPending(): Unit =
      if (pending.nonEmpty) {
        val scored = evaluate(pending.toSeq, baseline)
        pending.clear()
        scored.foreach(m => if (population.size < desiredSize) addToPopulation(m))
      }

    private def addToPopulation(member: ParameterArrayWithFitness): Unit = {
      population += member
      onAdded(member)
    }
  }

  /**
    * Absolute-fitness greedy neighbor search: up to `maxNeighborRetries + 1` evaluations, stopping at the first
    * neighbor that beats the parent. The parent's fitness is already known, so it is not evaluated again.
    */
  private def greedyNeighbor(parent: ParameterArrayWithFitness, radius: Double): ParameterArrayWithFitness =
    if (config.useMoves && parent.pa.supportsMoves) greedyNeighborByMoves(parent, radius)
    else greedyNeighborByBuilding(parent, radius)

  private def greedyNeighborByBuilding(parent: ParameterArrayWithFitness, radius: Double): ParameterArrayWithFitness = {
    var nbr = scoreAbsolute(parent.pa.getRandomNeighbor(radius))
    var retries = 0
    while (nbr.fitness >= parent.fitness && retries < config.maxNeighborRetries) {
      nbr = scoreAbsolute(parent.pa.getRandomNeighbor(radius))
      retries += 1
    }
    nbr
  }

  /** The same tries, with the same random numbers, but only the neighbor that is returned is ever built. */
  private def greedyNeighborByMoves(parent: ParameterArrayWithFitness, radius: Double): ParameterArrayWithFitness = {
    var move = parent.pa.proposeMove(radius)
    var fitness = optimizee.evaluateMove(parent.pa, parent.fitness, move)
    var retries = 0
    while (fitness >= parent.fitness && retries < config.maxNeighborRetries) {
      move = parent.pa.proposeMove(radius)
      fitness = optimizee.evaluateMove(parent.pa, parent.fitness, move)
      retries += 1
    }
    ParameterArrayWithFitness(parent.pa.applyMove(move), fitness)
  }

  private def scoreAbsolute(pa: ParameterArray): ParameterArrayWithFitness =
    ParameterArrayWithFitness(pa, optimizee.evaluateFitness(pa))
}

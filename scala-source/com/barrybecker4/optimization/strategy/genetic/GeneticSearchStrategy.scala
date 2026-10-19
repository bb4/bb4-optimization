// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness.given
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, ParameterCrossover}
import com.barrybecker4.optimization.strategy.OptimizationStrategy
import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStrategy

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object GeneticSearchStrategy {

  /** True when two parameter arrays represent the same candidate (not merely the same reference). */
  private[strategy] def sameSolution(a: ParameterArray, b: ParameterArray): Boolean =
    (a eq b) || (a.getClass == b.getClass && a.size == b.size &&
      (0 until a.size).forall(i => a.get(i).getValue == b.get(i).getValue))

  private[strategy] def containsSolution(
      population: Iterable[ParameterArrayWithFitness],
      pa: ParameterArray
  ): Boolean =
    population.exists(m => sameSolution(m.pa, pa))

  /** Hashable identity of a candidate; two arrays have equal keys exactly when [[sameSolution]] holds. */
  private[genetic] final case class SolutionKey(kind: Class[?], values: IndexedSeq[Double])

  private[genetic] def solutionKey(pa: ParameterArray): SolutionKey =
    // `+ 0.0` turns -0.0 into 0.0, which are the same value for sameSolution but differ when boxed.
    SolutionKey(pa.getClass, (0 until pa.size).map(i => pa.get(i).getValue + 0.0))
}

/**
  * Genetic Algorithm (evolutionary) optimization strategy.
  * Many different strategies are possible to alter the population for each successive iteration.
  * The 2 primary ones that I use here are unary mutation and cross-over.
  * See Chapter 6 in "How to Solve it: Modern Heuristics" for more info.
  *
  * '''Comparison mode (`evaluateByComparison == true`):''' every generation the population is re-scored against the
  * generation baseline, which is the best individual so far, so a member's fitness is its improvement over that
  * baseline (negative is better). The champion's fitness is kept as the ''cumulative'' improvement over the
  * initial guess, i.e. the sum of the per-generation improvements. For score-difference optimizees this equals
  * `compareFitness(champion, initialGuess)`.
  *
  * Budget: when the optimizee is wrapped with an evaluation budget, the best individual evaluated so far is
  * returned.
  *
  * @param optimizee the thing to be optimized.
  */
class GeneticSearchStrategy(
    optimizee: Optimizee,
    rnd: Random = MathUtil.RANDOM,
    private var searchConfig: GeneticSearchConfig = GeneticSearchConfig.default
) extends OptimizationStrategy(optimizee) {

  /** @deprecated use [[getConfig]] / [[setConfig]] */
  def config: GeneticSearchConfig = searchConfig

  def getConfig: GeneticSearchConfig = searchConfig

  def setConfig(c: GeneticSearchConfig): Unit = {
    searchConfig = c
  }

  /** If we don't improve by at least this amount between iterations, terminate.  */
  protected def improvementEpsilon: Double = searchConfig.improvementEpsilon

  def setImprovementEpsilon(eps: Double): Unit = {
    searchConfig = searchConfig.copy(improvementEpsilon = eps)
  }

  /** radius to look for neighbors in. Adapts as the search progresses; reset at the start of each run. */
  private var nbrRadius = searchConfig.nbrRadius

  /** This is the desired number of members to be maintained in the population at any time.
    * Might not always get this many if there are duplicates or the search space is small.
    */
  private var desiredPopulationSize = 0

  /** Best individual seen across generations (absolute or cumulative compare-mode scores). */
  private var globalChampion: Option[ParameterArrayWithFitness] = None

  /**
    * Compare mode only: the champion's cumulative fitness when the current generation (or the initial seeding,
    * where it is 0) started. Fitness scored against the baseline plus this offset is in the champion's frame.
    */
  private var baselineFitness = 0.0

  /** Finds a local minima using a genetic algorithm (evolutionary) search.
    * Stop iterating as soon as the average evaluation score of the population
    * does not significantly improve.
    * @param params the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    resetRunState(params)
    val seeded =
      try Some(findInitialPopulation(params))
      catch {
        case _: EvaluationBudgetExceededException => None
      }
    seeded match {
      case None => championOr(ParameterArrayWithFitness(params, Double.MaxValue))
      case Some(population) =>
        val start = population.min
        considerChampion(start)
        new Evolution(population, start, fitnessRange).run()
    }
  }

  private def resetRunState(params: ParameterArray): Unit = {
    desiredPopulationSize = params.getSamplePopulationSize
    globalChampion = None
    baselineFitness = 0.0
    nbrRadius = searchConfig.nbrRadius
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Initial population
  // ---------------------------------------------------------------------------------------------------------------

  /** Visible for tests in `com.barrybecker4.optimization`. */
  private[optimization] def findInitialPopulation(params: ParameterArray): ArrayBuffer[ParameterArrayWithFitness] = {
    if (desiredPopulationSize <= 0) desiredPopulationSize = params.getSamplePopulationSize
    val seedMember = evaluateSeed(params)
    considerChampion(seedMember)
    val population = ArrayBuffer(seedMember)

    val scored = evaluateOffspring(proposeInitialNeighbors(params), params)
    val seen = mutable.HashSet(GeneticSearchStrategy.solutionKey(params))
    scored.foreach { m =>
      if (seen.add(GeneticSearchStrategy.solutionKey(m.pa))) {
        population += m
        considerChampion(inChampionFrame(m))
      }
    }
    if (population.size <= 1) throw new IllegalStateException("No random neighbors found for " + params)
    population
  }

  /** In compare mode the seed is the baseline everything else is scored against, so its own score is 0. */
  private def evaluateSeed(params: ParameterArray): ParameterArrayWithFitness = {
    val fitness = if (optimizee.evaluateByComparison) 0.0 else optimizee.evaluateFitness(params)
    ParameterArrayWithFitness(params, fitness)
  }

  /** Distinct random neighbors of the seed, enough to fill the population (fewer if the space is small). */
  private def proposeInitialNeighbors(params: ParameterArray): Seq[ParameterArray] = {
    val seen = mutable.HashSet(GeneticSearchStrategy.solutionKey(params))
    val neighbors = ArrayBuffer[ParameterArray]()
    val maxAttempts = 100 * desiredPopulationSize
    var attempts = 0
    while (1 + neighbors.size < desiredPopulationSize && attempts < maxAttempts) {
      val nbr = params.getRandomNeighbor(searchConfig.initialNeighborRadius)
      if (seen.add(GeneticSearchStrategy.solutionKey(nbr))) neighbors += nbr
      attempts += 1
    }
    neighbors.toSeq
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Evolution
  // ---------------------------------------------------------------------------------------------------------------

  /** State of the generational loop: stops on optimum reached, stagnation, or the generation limit. */
  private final class Evolution(initialPopulation: ArrayBuffer[ParameterArrayWithFitness],
                                start: ParameterArrayWithFitness,
                                fitnessRange: Double) {
    private var population = initialPopulation
    private var generation = 0
    private var deltaFitness = -1.0
    private var stagnantGenerations = 0
    private val lowThresh = 0.005 * fitnessRange
    private val highThresh = 0.05 * fitnessRange

    def run(): ParameterArrayWithFitness = {
      var budgetExhausted = false
      try while (stopReason.isEmpty) nextGeneration()
      catch {
        case _: EvaluationBudgetExceededException => budgetExhausted = true
      }
      val result = championOr(start)
      if (!budgetExhausted) stopReason.foreach(reason => trace("stopped because " + reason))
      trace("----------------------- done -------------------")
      if (isLoggingToFile) log(generation, result, 0, 0, FormatUtil.formatNumber(generation))
      result
    }

    private def champion: ParameterArrayWithFitness = championOr(start)

    private def stopReason: Option[String] =
      if (isOptimalFitnessReached(champion)) Some("we found the optimal fitness.")
      else if (isStagnant && stagnantGenerations > searchConfig.maxStagnantGenerations)
        Some(s"we made no IMPROVEMENT. The delta, $deltaFitness was >= ${-improvementEpsilon}")
      else if (generation >= searchConfig.maxIterations)
        Some(s"we reached the MAX ITERATIONS(${searchConfig.maxIterations}): num iterations = $generation")
      else None

    private def isStagnant: Boolean = deltaFitness >= -improvementEpsilon

    private def nextGeneration(): Unit = {
      val before = champion
      baselineFitness = before.fitness
      reevaluatePopulationFitness(population, before.pa)
      population = cullPopulation(population)
      replaceCulledWithKeeperVariants(population, population.size, before.pa)
      ensureElite(population)
      considerChampion(inChampionFrame(population.min))

      // The champion never gets worse, so the change is 0 (no progress) or negative (progress).
      deltaFitness = math.min(champion.fitness - before.fitness, 0.0)
      log(generation, champion, nbrRadius, deltaFitness, "---")
      trace(s"currBest = $champion \nrecBest = $before        ct=$generation")
      trace(s"delta fitness =$deltaFitness      rbrRadius = $nbrRadius  improvementEpsilon = $improvementEpsilon")
      scaleNeighborRadius(deltaFitness, lowThresh, highThresh)
      notifyOfChange(champion)
      generation += 1
      if (isStagnant) stagnantGenerations += 1
    }
  }

  private def scaleNeighborRadius(deltaFitness: Double, lowThresh: Double, highThresh: Double): Unit = {
    val factor =
      if (deltaFitness < -highThresh) searchConfig.nbrRadiusExpandFactor
      else if (deltaFitness > -lowThresh) searchConfig.nbrRadiusShrinkFactor
      else 1.0
    nbrRadius = math.min(SimulatedAnnealingStrategy.MAX_PROPOSAL_RADIUS, nbrRadius * factor)
  }

  /** Mutation radius grows with the rank of the parent (worse parents take bigger steps). */
  private def effectiveMutationRadius(keeperIndex: Int): Double = {
    val softener = searchConfig.nbrRadiusSoftener
    val raw = (keeperIndex + softener) / softener * nbrRadius
    math.min(SimulatedAnnealingStrategy.MAX_PROPOSAL_RADIUS, raw)
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Champion tracking
  // ---------------------------------------------------------------------------------------------------------------

  private def considerChampion(candidate: ParameterArrayWithFitness): Unit = {
    updateRunBest(candidate)
    if (globalChampion.forall(_.fitness > candidate.fitness))
      globalChampion = Some(candidate)
  }

  private def championOr(fallback: ParameterArrayWithFitness): ParameterArrayWithFitness =
    globalChampion.getOrElse(fallback)

  /** Compare mode scores members against the generation baseline; shift them into the champion's frame. */
  private def inChampionFrame(member: ParameterArrayWithFitness): ParameterArrayWithFitness =
    if (optimizee.evaluateByComparison) member.copy(fitness = baselineFitness + member.fitness)
    else member

  /** The inverse of [[inChampionFrame]]: how the champion scores as a member of the current population. */
  private def inPopulationFrame(champion: ParameterArrayWithFitness): ParameterArrayWithFitness =
    if (optimizee.evaluateByComparison) champion.copy(fitness = champion.fitness - baselineFitness)
    else champion

  /** Reinject the champion if culling removed it, replacing the worst member when the population is full. */
  private def ensureElite(population: ArrayBuffer[ParameterArrayWithFitness]): Unit =
    if (searchConfig.elitismEnabled) {
      globalChampion.foreach { champ =>
        if (!GeneticSearchStrategy.containsSolution(population, champ.pa)) {
          val member = inPopulationFrame(champ)
          if (population.size < desiredPopulationSize) population += member
          else population(population.indices.maxBy(i => population(i).fitness)) = member
        }
      }
    }

  // ---------------------------------------------------------------------------------------------------------------
  // Culling and refilling
  // ---------------------------------------------------------------------------------------------------------------

  /** Remove all but the best candidates. Better candidates have lower values. */
  private def cullPopulation(population: ArrayBuffer[ParameterArrayWithFitness]): ArrayBuffer[ParameterArrayWithFitness] = {
    val sorted = population.sorted
    val keepSize = math.max(1, (sorted.size * (1.0 - searchConfig.cullFactor)).toInt)
    sorted.take(keepSize)
  }

  /**
    * Replace culled slots until `desiredPopulationSize` is reached.
    * Crossover / single-sample mutation proposals are batched through [[evaluateOffspring]];
    * greedy absolute mutation evaluates inline.
    */
  private def replaceCulledWithKeeperVariants(population: ArrayBuffer[ParameterArrayWithFitness],
                                              keepSize: Int,
                                              generationBaseline: ParameterArray): Unit =
    new Refill(population, keepSize, generationBaseline).run()

  /** One refill pass: proposes crossover children and mutants of the keepers until the population is full. */
  private final class Refill(population: ArrayBuffer[ParameterArrayWithFitness],
                             keepSize: Int,
                             generationBaseline: ParameterArray) {
    private val pending = ArrayBuffer[ParameterArray]()
    /** Everything in the population or pending, so duplicates are detected in constant time. */
    private val seen = mutable.HashSet.from(population.map(m => GeneticSearchStrategy.solutionKey(m.pa)))
    private val useGreedy =
      !optimizee.evaluateByComparison && searchConfig.neighborSelection == NeighborSelection.GreedyImproveAbsolute

    def run(): Unit = {
      val maxAttempts = math.max(100 * desiredPopulationSize, (desiredPopulationSize - keepSize) * 50 + 50)
      var attempts = 0
      while (population.size + pending.size < desiredPopulationSize && attempts < maxAttempts) {
        attempts += 1
        if (!queueCrossoverChild()) mutateKeeper()
      }
      flushPending()
    }

    private def shouldCross: Boolean =
      searchConfig.crossoverEnabled && rnd.nextDouble() < searchConfig.crossoverProbability && keepSize >= 2

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
      try queueIfNew(ParameterCrossover.cross(p1, p2, rnd))
      catch {
        case _: IllegalArgumentException => false // parents that cannot be crossed
      }

    private def queueIfNew(candidate: ParameterArray): Boolean = {
      val isNew = seen.add(GeneticSearchStrategy.solutionKey(candidate))
      if (isNew) pending += candidate
      isNew
    }

    private def mutateKeeper(): Unit = {
      val r = rnd.nextDouble()
      val keeperIndex = (r * r * keepSize).toInt
      val parent = population(keeperIndex)
      val radius = effectiveMutationRadius(keeperIndex)
      if (useGreedy) addGreedyMutant(parent, radius)
      else queueIfNew(parent.pa.getRandomNeighbor(radius))
    }

    private def addGreedyMutant(parent: ParameterArrayWithFitness, radius: Double): Unit = {
      flushPending()
      val mutant = getNeighborGreedy(parent, radius)
      if (seen.add(GeneticSearchStrategy.solutionKey(mutant.pa))) addToPopulation(mutant)
    }

    private def flushPending(): Unit =
      if (pending.nonEmpty) {
        val scored = evaluateOffspring(pending.toSeq, generationBaseline)
        pending.clear()
        scored.foreach(m => if (population.size < desiredPopulationSize) addToPopulation(m))
      }

    private def addToPopulation(member: ParameterArrayWithFitness): Unit = {
      population += member
      val inFrame = inChampionFrame(member)
      considerChampion(inFrame)
      notifyOfChange(inFrame)
    }
  }

  // ---------------------------------------------------------------------------------------------------------------
  // Fitness evaluation
  // ---------------------------------------------------------------------------------------------------------------

  /**
    * Evaluate a batch of candidate parameter arrays (sequential by default).
    * [[ConcurrentGeneticSearchStrategy]] overrides for parallel fitness evaluation.
    * Parent selection / crossover stay on the calling thread.
    *
    * If the evaluation budget runs out part way, the candidates scored so far are still recorded as champion
    * candidates before the exception propagates.
    */
  protected def evaluateOffspring(
      candidates: Seq[ParameterArray],
      generationBaseline: ParameterArray
  ): Seq[ParameterArrayWithFitness] = {
    val scored = ArrayBuffer[ParameterArrayWithFitness]()
    try candidates.foreach(pa => scored += evaluateParameterArray(pa, generationBaseline))
    catch {
      case e: EvaluationBudgetExceededException =>
        recordPartialResults(scored)
        throw e
    }
    scored.toSeq
  }

  /** For subclasses: keep what was learned from the candidates scored before an evaluation failed. */
  protected final def recordPartialResults(scored: Iterable[ParameterArrayWithFitness]): Unit =
    scored.foreach(m => considerChampion(inChampionFrame(m)))

  protected def evaluateParameterArray(pa: ParameterArray, generationBaseline: ParameterArray): ParameterArrayWithFitness = {
    val fitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(pa, generationBaseline)
      else optimizee.evaluateFitness(pa)
    ParameterArrayWithFitness(pa, fitness)
  }

  /**
    * Absolute-fitness greedy neighbor search: up to `maxNeighborRetries + 1` evaluations, stopping at the first
    * neighbor that beats the parent. The parent's fitness is already known, so it is not evaluated again.
    */
  private def getNeighborGreedy(parent: ParameterArrayWithFitness, radius: Double): ParameterArrayWithFitness = {
    var nbr = scoreAbsolute(parent.pa.getRandomNeighbor(radius))
    var retries = 0
    while (nbr.fitness >= parent.fitness && retries < searchConfig.maxNeighborRetries) {
      nbr = scoreAbsolute(parent.pa.getRandomNeighbor(radius))
      retries += 1
    }
    nbr
  }

  private def scoreAbsolute(pa: ParameterArray): ParameterArrayWithFitness =
    ParameterArrayWithFitness(pa, optimizee.evaluateFitness(pa))

  /** Refresh per-individual fitness when using relative fitness vs the previous generation best. */
  protected def reevaluatePopulationFitness(population: ArrayBuffer[ParameterArrayWithFitness],
                                            baseline: ParameterArray): Unit =
    if (optimizee.evaluateByComparison) {
      for (i <- population.indices) {
        val pa = population(i).pa
        population(i) = ParameterArrayWithFitness(pa, optimizee.compareFitness(pa, baseline))
      }
    }

  // ---------------------------------------------------------------------------------------------------------------
  // Test hooks
  // ---------------------------------------------------------------------------------------------------------------

  /** Runs one refill pass as in the generation loop; visible for tests. */
  private[optimization] def refillAfterCullForTesting(
      population: ArrayBuffer[ParameterArrayWithFitness],
      keepSize: Int,
      generationBaseline: ParameterArray,
      desiredPopSize: Int
  ): Unit = {
    val saved = desiredPopulationSize
    desiredPopulationSize = desiredPopSize
    try replaceCulledWithKeeperVariants(population, keepSize, generationBaseline)
    finally desiredPopulationSize = saved
  }

  /** Inject elitism for tests after a cull. */
  private[optimization] def ensureEliteForTesting(population: ArrayBuffer[ParameterArrayWithFitness]): Unit =
    ensureElite(population)

  private[optimization] def setGlobalChampionForTesting(c: ParameterArrayWithFitness): Unit = {
    globalChampion = Some(c)
    updateRunBest(c)
  }
}

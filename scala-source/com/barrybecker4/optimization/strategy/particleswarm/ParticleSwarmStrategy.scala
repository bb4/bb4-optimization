// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategy

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
 * Particle Swarm Optimization (PSO) strategy for continuous (and integer-valued) numeric parameter spaces.
 *
 * A swarm of particles flies through the bounded parameter space. Each particle remembers the best position it has
 * personally visited and is pulled toward that position (cognitive term) and toward the best position known in its
 * neighborhood (social term), while keeping some of its previous velocity (inertia):
 * {{{
 *   v = w * v + c1 * r1 * (personalBest - x) + c2 * r2 * (neighborhoodBest - x)
 *   x = x + v
 * }}}
 * where r1 and r2 are fresh uniform random numbers in [0, 1) for every particle and dimension.
 * See [[ParticleSwarmConfig]] for the tunables and [[ParticleSwarmTopology]] for how neighborhoods are defined.
 *
 * Good for non-linear, multimodal, non-differentiable objectives where a reasonable population-based search is
 * wanted without having to tune a cooling schedule. Like [[DifferentialEvolutionStrategy]] it needs no gradient.
 *
 * Details:
 *  - The initial guess becomes particle 0; the other particles start uniformly at random within each parameter's
 *    bounds, so the initial guess's bounds define the search space (as with the other strategies).
 *  - Particles are never allowed outside the parameter bounds. A particle that hits a wall is placed on the wall
 *    and loses its velocity in that dimension.
 *  - Updates are synchronous: all particles in an iteration are attracted to the personal bests as they were at the
 *    start of that iteration. Given the same `rnd` seed and the same inputs a run is reproducible.
 *  - Integer-valued parameters are rounded to the nearest integer.
 *  - The run ends at `maxIterations`, when the optimal fitness is reached, or when the best fitness has not improved
 *    meaningfully for `stagnationIterations` iterations (see [[ParticleSwarmConfig]]).
 *
 * Limitations: the fitness must be absolute (`evaluateByComparison == false`) and the parameters must be a
 * [[NumericParameterArray]]; permutations and variable-length sets are not supported (use a genetic or annealing
 * strategy for those). Violations are reported with an `IllegalArgumentException`.
 *
 * Budget: when the optimizee is wrapped with an evaluation budget, this strategy catches
 * [[EvaluationBudgetExceededException]] and returns the best position found so far.
 *
 * @param optimizee the thing to be optimized.
 * @param rnd       source of randomness for the initial swarm and the stochastic velocity updates.
 */
class ParticleSwarmStrategy(optimizee: Optimizee, rnd: Random = MathUtil.RANDOM)
  extends OptimizationStrategy(optimizee) {

  private var config: ParticleSwarmConfig = ParticleSwarmConfig.Default

  def getConfig: ParticleSwarmConfig = config

  def setConfig(config: ParticleSwarmConfig): Unit = {
    this.config = config
  }

  /**
   * @param params       the initial guess; becomes particle 0 and defines the bounds of the search space.
   * @param fitnessRange approximate scale of the objective. Only used to turn the relative `stagnationTolerance`
   *                     into an absolute fitness difference; if it is not a positive finite number the tolerance
   *                     is used as an absolute value.
   * @return the best parameters found, with their fitness.
   */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    require(!optimizee.evaluateByComparison,
      "ParticleSwarmStrategy needs an absolute fitness; optimizees with evaluateByComparison == true are not supported")
    val template = requireNumeric(params)
    require(template.size > 0, "ParticleSwarmStrategy needs at least one parameter to optimize")

    new SwarmRun(template, config, fitnessRange).execute().getOrElse(ParameterArrayWithFitness(params, Double.MaxValue))
  }

  private def requireNumeric(params: ParameterArray): NumericParameterArray = params match {
    case numeric: NumericParameterArray => numeric
    case other => throw new IllegalArgumentException(
      "ParticleSwarmStrategy only supports NumericParameterArray, but got " + other.getClass.getSimpleName)
  }

  /** One run of the swarm. If the evaluation budget runs out, the best position evaluated so far is kept. */
  private final class SwarmRun(template: NumericParameterArray, cfg: ParticleSwarmConfig, fitnessRange: Double) {

    private val space = new SearchSpace(template, cfg.maxVelocityFraction)
    private val tolerance =
      if (fitnessRange > 0.0 && fitnessRange.isFinite) cfg.stagnationTolerance * fitnessRange
      else cfg.stagnationTolerance
    private val swarm = ArrayBuffer.empty[Particle]
    private var best: Option[ParameterArrayWithFitness] = None

    /** @return the best position found, or None if not even one position could be evaluated */
    def execute(): Option[ParameterArrayWithFitness] = {
      try {
        seedSwarm()
        iterate()
      } catch {
        case _: EvaluationBudgetExceededException => () // fall through and return the best found so far
      }
      best
    }

    /** The initial guess plus uniformly random particles. */
    private def seedSwarm(): Unit =
      for (i <- 0 until cfg.swarmSize) {
        val start = if (i == 0) space.initialGuessPosition else space.randomPosition(rnd)
        val candidate = space.toParameterArray(start)
        val position = space.positionOf(candidate)
        val velocity = space.randomVelocity(rnd, cfg.initialVelocityFraction)
        val fitness = evaluate(candidate)
        swarm += new Particle(position, velocity, position, fitness)
        recordIfBest(candidate, fitness)
      }

    private def iterate(): Unit = {
      var iteration = 0
      var stagnantIterations = 0
      while (!finished(iteration, stagnantIterations)) {
        iteration += 1
        val improvement = runIteration(iteration)
        stagnantIterations = if (improvement > tolerance) 0 else stagnantIterations + 1
      }
    }

    private def finished(iteration: Int, stagnantIterations: Int): Boolean =
      iteration >= cfg.maxIterations ||
        (cfg.stagnationIterations > 0 && stagnantIterations >= cfg.stagnationIterations) ||
        isOptimalFitnessReached(best.get)

    /** Moves every particle once. @return how much the best fitness improved */
    private def runIteration(iteration: Int): Double = {
      val previousBestFitness = best.get.fitness
      val attractors = SocialAttractors(swarm, cfg.topology)
      swarm.zip(attractors).foreach { case (particle, attractor) => moveParticle(particle, attractor) }
      val improvement = previousBestFitness - best.get.fitness
      log(iteration, best.get, 0.0, improvement, "swarm")
      trace("iteration " + iteration + " best fitness = " + best.get.fitness)
      improvement
    }

    private def moveParticle(particle: Particle, attractor: Array[Double]): Unit = {
      val candidate = space.toParameterArray(particle.nextPosition(attractor, space, cfg, rnd))
      val fitness = evaluate(candidate)
      particle.arriveAt(space.positionOf(candidate), fitness)
      recordIfBest(candidate, fitness)
    }

    private def evaluate(candidate: ParameterArray): Double = {
      val fitness = optimizee.evaluateFitness(candidate)
      if (fitness.isNaN) Double.PositiveInfinity else fitness // a NaN must never look like an improvement
    }

    private def recordIfBest(candidate: ParameterArray, fitness: Double): Unit =
      if (best.forall(current => fitness < current.fitness)) {
        val improved = ParameterArrayWithFitness(candidate, fitness)
        best = Some(improved)
        notifyOfChange(improved)
      }
  }
}

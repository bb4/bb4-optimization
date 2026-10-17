// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

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

  /** The mutable state of one particle. `position` and `bestPosition` arrays are never modified in place
    * (they are replaced), so a reference to a best position stays valid for the rest of an iteration. */
  private final class Particle(
      var position: Array[Double],
      val velocity: Array[Double],
      var bestPosition: Array[Double],
      var bestFitness: Double
  )

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
    val template: NumericParameterArray = params match {
      case numeric: NumericParameterArray => numeric
      case other => throw new IllegalArgumentException(
        "ParticleSwarmStrategy only supports NumericParameterArray, but got " + other.getClass.getSimpleName)
    }
    require(template.size > 0, "ParticleSwarmStrategy needs at least one parameter to optimize")

    val cfg = config
    val n = template.size
    val lower = Array.tabulate(n)(j => template.get(j).minValue)
    val upper = Array.tabulate(n)(j => template.get(j).maxValue)
    val range = Array.tabulate(n)(j => upper(j) - lower(j))
    val maxVelocity = Array.tabulate(n)(j => cfg.maxVelocityFraction * range(j))
    val tolerance =
      if (fitnessRange > 0.0 && fitnessRange.isFinite) cfg.stagnationTolerance * fitnessRange
      else cfg.stagnationTolerance

    val swarm = ArrayBuffer.empty[Particle]
    var best: Option[ParameterArrayWithFitness] = None

    def evaluate(candidate: ParameterArray): Double = {
      val fitness = optimizee.evaluateFitness(candidate)
      if (fitness.isNaN) Double.PositiveInfinity else fitness // a NaN must never look like an improvement
    }

    def recordIfBest(candidate: ParameterArray, fitness: Double): Unit =
      if (best.forall(current => fitness < current.fitness)) {
        val improved = ParameterArrayWithFitness(candidate, fitness)
        best = Some(improved)
        notifyOfChange(improved)
      }

    try {
      // Initial swarm: the initial guess plus uniformly random particles.
      var i = 0
      while (i < cfg.swarmSize) {
        val start =
          if (i == 0) positionOf(template)
          else Array.tabulate(n)(j => lower(j) + rnd.nextDouble() * range(j))
        val candidate = toParameterArray(template, start)
        val position = positionOf(candidate)
        val velocity = Array.tabulate(n)(j => (2.0 * rnd.nextDouble() - 1.0) * cfg.initialVelocityFraction * range(j))
        val fitness = evaluate(candidate)
        swarm += new Particle(position, velocity, position, fitness)
        recordIfBest(candidate, fitness)
        i += 1
      }

      var iteration = 0
      var stagnantIterations = 0
      while (iteration < cfg.maxIterations
          && (cfg.stagnationIterations <= 0 || stagnantIterations < cfg.stagnationIterations)
          && !isOptimalFitnessReached(best.get)) {

        val previousBestFitness = best.get.fitness
        val attractors = socialBestPositions(swarm, cfg.topology)

        var k = 0
        while (k < swarm.size) {
          val particle = swarm(k)
          val attractor = attractors(k)
          val next = new Array[Double](n)
          var j = 0
          while (j < n) {
            val towardOwnBest = particle.bestPosition(j) - particle.position(j)
            val towardSocialBest = attractor(j) - particle.position(j)
            var v = cfg.inertiaWeight * particle.velocity(j) +
              cfg.cognitiveWeight * rnd.nextDouble() * towardOwnBest +
              cfg.socialWeight * rnd.nextDouble() * towardSocialBest
            v = math.max(-maxVelocity(j), math.min(maxVelocity(j), v))
            var x = particle.position(j) + v
            if (x < lower(j)) {
              x = lower(j)
              v = 0.0
            } else if (x > upper(j)) {
              x = upper(j)
              v = 0.0
            }
            particle.velocity(j) = v
            next(j) = x
            j += 1
          }

          val candidate = toParameterArray(template, next)
          particle.position = positionOf(candidate)
          val fitness = evaluate(candidate)
          if (fitness < particle.bestFitness) {
            particle.bestPosition = particle.position
            particle.bestFitness = fitness
          }
          recordIfBest(candidate, fitness)
          k += 1
        }

        iteration += 1
        val improvement = previousBestFitness - best.get.fitness
        stagnantIterations = if (improvement > tolerance) 0 else stagnantIterations + 1
        log(iteration, best.get, 0.0, improvement, "swarm")
        trace("iteration " + iteration + " best fitness = " + best.get.fitness)
      }
    } catch {
      case _: EvaluationBudgetExceededException =>
        () // fall through and return the best found so far
    }

    best.getOrElse(ParameterArrayWithFitness(params, Double.MaxValue))
  }

  /** @return for each particle, the position it is attracted to socially (never modified by the caller). */
  private def socialBestPositions(
      swarm: ArrayBuffer[Particle],
      topology: ParticleSwarmTopology
  ): IndexedSeq[Array[Double]] = {
    val size = swarm.size
    topology match {
      case ParticleSwarmTopology.Global =>
        val leader = swarm.minBy(_.bestFitness)
        IndexedSeq.fill(size)(leader.bestPosition)
      case ParticleSwarmTopology.Ring =>
        IndexedSeq.tabulate(size) { i =>
          val neighborhood = Seq(swarm(i), swarm((i + size - 1) % size), swarm((i + 1) % size))
          neighborhood.minBy(_.bestFitness).bestPosition
        }
    }
  }

  /** @return the values of the parameter array as plain doubles. */
  private def positionOf(pa: ParameterArray): Array[Double] =
    Array.tabulate(pa.size)(j => pa.get(j).getValue)

  /**
    * Builds a new parameter array with the given values. The parameters keep their bounds and redistribution
    * function from the template. Integer parameters are rounded (their `setValue` would otherwise truncate).
    */
  private def toParameterArray(template: NumericParameterArray, values: Array[Double]): NumericParameterArray = {
    val parameters = IndexedSeq.tabulate(values.length) { j =>
      val parameter = template.get(j)
      parameter.setValue(if (parameter.isIntegerOnly) math.rint(values(j)) else values(j))
    }
    NumericParameterArray(parameters, template.numSteps, template.rnd)
  }
}

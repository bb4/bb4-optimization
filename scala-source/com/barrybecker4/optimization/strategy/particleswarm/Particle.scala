// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

import scala.util.Random

/**
 * The mutable state of one particle. `position` and `bestPosition` arrays are never modified in place (they are
 * replaced), so a reference to a best position stays valid for the rest of an iteration.
 */
private[particleswarm] final class Particle(
    var position: Array[Double],
    val velocity: Array[Double],
    var bestPosition: Array[Double],
    var bestFitness: Double
) {

  /**
   * Updates the velocity and returns the position to move to:
   * {{{
   *   v = w * v + c1 * r1 * (personalBest - x) + c2 * r2 * (attractor - x)    (then clamped to the speed limit)
   *   x = x + v
   * }}}
   * A particle that would leave the box is placed on the wall and loses its velocity in that dimension.
   * The particle's `position` is not changed; call [[arriveAt]] once the new position has been evaluated.
   *
   * @param attractor the best position known in this particle's neighborhood
   */
  def nextPosition(attractor: Array[Double], space: SearchSpace, cfg: ParticleSwarmConfig, rnd: Random): Array[Double] = {
    val next = new Array[Double](position.length)
    var j = 0
    while (j < next.length) {
      next(j) = nextCoordinate(j, attractor(j), space, cfg, rnd)
      j += 1
    }
    next
  }

  /** Records where the particle ended up and how good it was; keeps the personal best up to date. */
  def arriveAt(newPosition: Array[Double], fitness: Double): Unit = {
    position = newPosition
    if (fitness < bestFitness) {
      bestPosition = newPosition
      bestFitness = fitness
    }
  }

  private def nextCoordinate(j: Int, attractorValue: Double, space: SearchSpace,
                             cfg: ParticleSwarmConfig, rnd: Random): Double = {
    val towardOwnBest = bestPosition(j) - position(j)
    val towardSocialBest = attractorValue - position(j)
    val v = space.clampVelocity(j,
      cfg.inertiaWeight * velocity(j) +
        cfg.cognitiveWeight * rnd.nextDouble() * towardOwnBest +
        cfg.socialWeight * rnd.nextDouble() * towardSocialBest)
    val x = position(j) + v
    if (x < space.lower(j)) stopAt(j, space.lower(j))
    else if (x > space.upper(j)) stopAt(j, space.upper(j))
    else {
      velocity(j) = v
      x
    }
  }

  private def stopAt(j: Int, wall: Double): Double = {
    velocity(j) = 0.0
    wall
  }
}

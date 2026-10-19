// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

/** Decides which best position each particle is pulled toward socially, according to the topology. */
private[particleswarm] object SocialAttractors {

  /** @return for each particle, the position it is attracted to socially (never modified by the caller). */
  def apply(swarm: scala.collection.IndexedSeq[Particle], topology: ParticleSwarmTopology): IndexedSeq[Array[Double]] =
    topology match {
      case ParticleSwarmTopology.Global =>
        val leader = swarm.minBy(_.bestFitness)
        IndexedSeq.fill(swarm.size)(leader.bestPosition)
      case ParticleSwarmTopology.Ring =>
        IndexedSeq.tabulate(swarm.size)(i => ringNeighborhoodBest(swarm, i).bestPosition)
    }

  /** The best of particle `i` and its two ring neighbors; on a tie the particle itself, then its predecessor, wins. */
  private def ringNeighborhoodBest(swarm: scala.collection.IndexedSeq[Particle], i: Int): Particle = {
    val size = swarm.size
    val candidates = Iterator(swarm(i), swarm((i + size - 1) % size), swarm((i + 1) % size))
    candidates.reduceLeft((best, next) => if (next.bestFitness < best.bestFitness) next else best)
  }
}

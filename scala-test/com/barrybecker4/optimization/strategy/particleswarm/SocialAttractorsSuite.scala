package com.barrybecker4.optimization.strategy.particleswarm

import org.scalatest.funsuite.AnyFunSuite

class SocialAttractorsSuite extends AnyFunSuite {

  /** Particle i has personal best position Array(i) with the given fitness. */
  private def swarmWithBestFitnesses(fitnesses: Double*): IndexedSeq[Particle] =
    fitnesses.zipWithIndex.map { case (f, i) => new Particle(Array(i.toDouble), Array(0.0), Array(i.toDouble), f) }.toIndexedSeq

  private def indices(attractors: IndexedSeq[Array[Double]]): Seq[Int] = attractors.map(_(0).toInt)

  test("global topology points everyone at the best personal best of the whole swarm") {
    val swarm = swarmWithBestFitnesses(5.0, 1.0, 4.0, 3.0, 2.0)
    assert(indices(SocialAttractors(swarm, ParticleSwarmTopology.Global)) == Seq(1, 1, 1, 1, 1))
  }

  test("ring topology points each particle at the best among itself and its two neighbors, wrapping around") {
    val swarm = swarmWithBestFitnesses(5.0, 1.0, 4.0, 3.0, 2.0)
    // 0: {4, 0, 1} -> 1;  1: {0, 1, 2} -> 1;  2: {1, 2, 3} -> 1;  3: {2, 3, 4} -> 4;  4: {3, 4, 0} -> 4
    assert(indices(SocialAttractors(swarm, ParticleSwarmTopology.Ring)) == Seq(1, 1, 1, 4, 4))
  }

  test("ring information does not jump past the neighbors") {
    val swarm = swarmWithBestFitnesses(9.0, 9.0, 9.0, 9.0, 0.0, 9.0, 9.0, 9.0)
    // only particles 3, 4 and 5 can see the leader at index 4; the others keep themselves
    assert(indices(SocialAttractors(swarm, ParticleSwarmTopology.Ring)) == Seq(0, 1, 2, 4, 4, 4, 6, 7))
  }

  test("on a tie the particle keeps its own best, then prefers its predecessor") {
    val flat = swarmWithBestFitnesses(1.0, 1.0, 1.0)
    assert(indices(SocialAttractors(flat, ParticleSwarmTopology.Ring)) == Seq(0, 1, 2))

    val tiedNeighbors = swarmWithBestFitnesses(5.0, 2.0, 5.0, 2.0)
    // particle 0 sees 3 (predecessor) and 1 (successor), both fitness 2: the predecessor wins
    assert(SocialAttractors(tiedNeighbors, ParticleSwarmTopology.Ring)(0)(0) == 3.0)
  }

  test("a single particle and a pair are their own neighborhood or share it") {
    val one = swarmWithBestFitnesses(3.0)
    assert(indices(SocialAttractors(one, ParticleSwarmTopology.Ring)) == Seq(0))
    assert(indices(SocialAttractors(one, ParticleSwarmTopology.Global)) == Seq(0))

    val two = swarmWithBestFitnesses(3.0, 1.0)
    assert(indices(SocialAttractors(two, ParticleSwarmTopology.Ring)) == Seq(1, 1))
  }

  test("the attractor is the personal best position itself, not a copy") {
    val swarm = swarmWithBestFitnesses(2.0, 1.0)
    assert(SocialAttractors(swarm, ParticleSwarmTopology.Global)(0) eq swarm(1).bestPosition)
  }
}

package com.barrybecker4.optimization.strategy.particleswarm

import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class ParticleSuite extends AnyFunSuite {

  /** A random number generator whose draws are all the same, so the velocity update can be computed by hand. */
  private def constantRandom(value: Double): Random = new Random(new java.util.Random() {
    override def nextDouble(): Double = value
  })

  // box [0, 10] with a speed limit of 5; w = 0.5, c1 = 2, c2 = 4 and r1 = r2 = 0.5
  private val space = new SearchSpace(
    NumericParameterArray(IndexedSeq(DoubleParameter(5.0, 0.0, 10.0, "x")), rnd = new Random(1)),
    maxVelocityFraction = 0.5)
  private val cfg = ParticleSwarmConfig(inertiaWeight = 0.5, cognitiveWeight = 2.0, socialWeight = 4.0)

  private def particle(position: Double, velocity: Double, best: Double) =
    new Particle(Array(position), Array(velocity), Array(best), 1.0)

  test("a velocity above the speed limit is clamped to it") {
    val p = particle(position = 2.0, velocity = 1.0, best = 4.0)
    val next = p.nextPosition(Array(5.0), space, cfg, constantRandom(0.5))
    // v = 0.5 * 1 + 2 * 0.5 * (4 - 2) + 4 * 0.5 * (5 - 2) = 8.5, limited to 0.5 * 10 = 5
    assert(p.velocity(0) == 5.0)
    assert(next.toSeq == Seq(7.0))
  }

  test("the new velocity combines inertia, the pull toward the personal best and the pull toward the attractor") {
    val p = particle(position = 2.0, velocity = 1.0, best = 3.0)
    val gentle = ParticleSwarmConfig(inertiaWeight = 0.5, cognitiveWeight = 1.0, socialWeight = 1.0)
    val next = p.nextPosition(Array(4.0), space, gentle, constantRandom(0.5))
    // v = 0.5 * 1 + 1 * 0.5 * (3 - 2) + 1 * 0.5 * (4 - 2) = 2
    assert(p.velocity(0) == 2.0)
    assert(next.toSeq == Seq(4.0))
  }

  test("a particle that would cross the upper wall stops on it and loses its velocity") {
    val p = particle(position = 8.0, velocity = 4.0, best = 10.0)
    val next = p.nextPosition(Array(10.0), space, cfg, constantRandom(0.5))
    assert(next.toSeq == Seq(10.0))
    assert(p.velocity(0) == 0.0)
  }

  test("a particle that would cross the lower wall stops on it and loses its velocity") {
    val p = particle(position = 2.0, velocity = -4.0, best = 0.0)
    val next = p.nextPosition(Array(0.0), space, cfg, constantRandom(0.5))
    assert(next.toSeq == Seq(0.0))
    assert(p.velocity(0) == 0.0)
  }

  test("nextPosition leaves the position and personal best untouched until the particle arrives") {
    val p = particle(position = 2.0, velocity = 1.0, best = 4.0)
    val oldPosition = p.position
    p.nextPosition(Array(5.0), space, cfg, constantRandom(0.5))
    assert(p.position eq oldPosition)
    assert(p.position.toSeq == Seq(2.0))
    assert(p.bestPosition.toSeq == Seq(4.0))
  }

  test("arriving always moves the particle but only a strictly better fitness replaces the personal best") {
    val p = particle(position = 2.0, velocity = 0.0, best = 4.0) // bestFitness = 1.0
    val worse = Array(6.0)
    p.arriveAt(worse, fitness = 3.0)
    assert(p.position eq worse)
    assert(p.bestPosition.toSeq == Seq(4.0))
    assert(p.bestFitness == 1.0)

    p.arriveAt(Array(7.0), fitness = 1.0) // a tie keeps the old best
    assert(p.bestPosition.toSeq == Seq(4.0))

    val better = Array(8.0)
    p.arriveAt(better, fitness = 0.5)
    assert(p.bestPosition eq better)
    assert(p.bestFitness == 0.5)
  }

  test("each dimension is moved independently, with fresh random numbers") {
    val twoD = new SearchSpace(
      NumericParameterArray(
        IndexedSeq(DoubleParameter(5.0, 0.0, 10.0, "x"), DoubleParameter(0.0, -1.0, 1.0, "y")), rnd = new Random(1)),
      maxVelocityFraction = 0.5)
    val p = new Particle(Array(2.0, 0.0), Array(0.0, 0.0), Array(2.0, 0.0), 1.0)
    val next = p.nextPosition(Array(4.0, 1.0), twoD, ParticleSwarmConfig(socialWeight = 2.0, cognitiveWeight = 0.0),
      constantRandom(0.5))
    // x: v = 2 * 0.5 * (4 - 2) = 2;  y: v = 2 * 0.5 * (1 - 0) = 1, exactly its speed limit of 0.5 * 2
    assert(next(0) == 4.0)
    assert(next(1) == 1.0)
  }

  test("search space rounds integer parameters and keeps real ones as they are") {
    val mixed = new SearchSpace(
      NumericParameterArray(
        IndexedSeq(IntegerParameter(1, 0, 20, "i"), DoubleParameter(0.5, 0.0, 1.0, "d")), rnd = new Random(1)),
      maxVelocityFraction = 0.5)
    val pa = mixed.toParameterArray(Array(2.6, 0.3))
    assert(pa.get(0).getValue == 3.0)
    assert(pa.get(1).getValue == 0.3)
    assert(mixed.positionOf(pa).toSeq == Seq(3.0, 0.3))
  }

  test("random positions and velocities respect the box and the initial velocity fraction") {
    val rnd = new Random(3)
    for (_ <- 1 to 200) {
      val pos = space.randomPosition(rnd)(0)
      assert(pos >= 0.0 && pos <= 10.0)
      val vel = space.randomVelocity(rnd, fraction = 0.1)(0)
      assert(vel >= -1.0 && vel <= 1.0)
    }
    assert(space.randomVelocity(rnd, fraction = 0.0).toSeq == Seq(0.0))
  }
}

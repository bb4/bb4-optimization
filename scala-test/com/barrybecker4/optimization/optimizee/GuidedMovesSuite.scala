// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, Swap}
import com.barrybecker4.optimization.strategy.annealing.{SimulatedAnnealingConfig, SimulatedAnnealingStrategy}
import com.barrybecker4.optimization.strategy.genetic.{GeneticSearchConfig, GeneticSearchStrategy}
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** A guided move is the problem's own suggestion, mixed in with random moves by a configurable probability. */
class GuidedMovesSuite extends AnyFunSuite {

  /** Counts how often it is asked; it suggests a swap of the first two positions, or nothing at all. */
  private class Guide(suggest: Boolean) extends RandomTourProblem(20, 5) {
    var asked = 0
    override def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] = {
      asked += 1
      if (suggest) Some(Swap(0, 1)) else None
    }
  }

  private val problem = new RandomTourProblem(20, 5)
  private val tour = problem.getInitialGuess

  test("with probability 0 the problem is never asked, and no random number is drawn") {
    val guide = new Guide(true)
    val rnd = new Random(7)
    MoveChoice.propose(guide, tour, 0.5, 0.0, rnd)
    assertResult(0)(guide.asked)
    // random moves draw from the array's own generator, so the one passed in is untouched
    assertResult(new Random(7).nextInt())(rnd.nextInt())
  }

  test("with probability 1 the problem's move is used") {
    val guide = new Guide(true)
    assertResult(Swap(0, 1))(MoveChoice.propose(guide, tour, 0.5, 1.0, new Random(1)))
    assertResult(1)(guide.asked)
  }

  test("a problem with nothing to suggest falls back to a random move") {
    val guide = new Guide(false)
    val move = MoveChoice.propose(guide, tour, 0.5, 1.0, new Random(1))
    assertResult(1)(guide.asked)
    assert(tour.applyMove(move) != null)
  }

  test("the probability is how often the problem is asked") {
    val guide = new Guide(true)
    val rnd = new Random(3)
    (1 to 2000).foreach(_ => MoveChoice.propose(guide, tour, 0.5, 0.25, rnd))
    assert(guide.asked > 400 && guide.asked < 600, s"asked ${guide.asked} of 2000")
  }

  test("a probability outside [0, 1] is refused") {
    intercept[IllegalArgumentException](SimulatedAnnealingConfig(guidedMoveProbability = 1.5))
    intercept[IllegalArgumentException](GeneticSearchConfig(guidedMoveProbability = -0.1))
  }

  private def annealed(guide: Guide, p: Double): Double = {
    val sa = new SimulatedAnnealingStrategy(guide, new Random(1))
    sa.setConfig(SimulatedAnnealingConfig(guidedMoveProbability = p))
    sa.doOptimization(guide.getInitialGuess, guide.getFitnessRange).fitness
  }

  test("simulated annealing asks the problem only when configured to") {
    val never = new Guide(true)
    annealed(never, 0.0)
    assertResult(0)(never.asked)
    val sometimes = new Guide(true)
    annealed(sometimes, 0.5)
    assert(sometimes.asked > 0)
  }

  test("simulated annealing with probability 0 is the same run as without a guide") {
    assertResult(annealed(new Guide(false), 0.0))(annealed(new Guide(true), 0.0))
  }

  test("genetic search asks the problem for greedy mutants only when configured to") {
    val asked = (p: Double) => {
      val guide = new Guide(true)
      val config = GeneticSearchConfig(crossoverEnabled = false, guidedMoveProbability = p)
      new GeneticSearchStrategy(guide, new Random(2), config).doOptimization(guide.getInitialGuess, guide.getFitnessRange)
      guide.asked
    }
    assertResult(0)(asked(0.0))
    assert(asked(1.0) > 0)
  }
}

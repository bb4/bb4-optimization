// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.optimizee.DeltaCheckingOptimizee
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, PuzzleLikePath, Swap}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/** A greedy mutant of a tour is found by trying moves, and only the mutant that is kept is built. */
class GreedyMutationMovesSuite extends AnyFunSuite {

  private class CountingTour(cities: Int, seed: Long) extends RandomTourProblem(cities, seed) {
    var fullEvaluations = 0
    var moveEvaluations = 0
    override def evaluateFitness(tour: ParameterArray): Double = { fullEvaluations += 1; super.evaluateFitness(tour) }
    override def evaluateMove(tour: ParameterArray, length: Double, move: Move): Double = {
      moveEvaluations += 1
      super.evaluateMove(tour, length, move)
    }
  }

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  /** Refills a population of three tours with greedy mutants, and returns the members that were added. */
  private def mutants(optimizee: RandomTourProblem, cities: Int, useMoves: Boolean, seed: Int): Seq[ParameterArrayWithFitness] = {
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val config = GeneticSearchConfig(crossoverEnabled = false, useMoves = useMoves)
    val refiller = new PopulationRefiller(optimizee, config, new Random(seed), (_, _) => Seq.empty, added += _)
    val population = ArrayBuffer((1 to 3).map { k =>
      val tour = new RandomTourProblem(cities, 100L + k).getInitialGuess.asInstanceOf[PermutedParameterArray]
        .forkWithRnd(new Random(seed * 10L + k))
      ParameterArrayWithFitness(tour, optimizee.evaluateFitness(tour))
    }*)
    refiller.refill(population, 3, 40, population.head.pa, 0.1)
    added.toSeq
  }

  test("the mutants are the same whether or not moves are used") {
    for (cities <- Seq(10, 50, 200); seed <- 1 to 4) {
      val problem = new RandomTourProblem(cities, 7)
      val withMoves = mutants(problem, cities, useMoves = true, seed)
      val without = mutants(problem, cities, useMoves = false, seed)
      assertResult(without.map(m => valuesOf(m.pa)), s"$cities cities, seed $seed")(withMoves.map(m => valuesOf(m.pa)))
    }
  }

  test("the fitness of each mutant is the length of its tour, with every delta checked") {
    val problem = new RandomTourProblem(60, 5)
    mutants(problem, 60, useMoves = true, 3).foreach { m =>
      assert(Math.abs(m.fitness - problem.evaluateFitness(m.pa)) < 1e-9)
    }
    // the same run with each move checked against a full evaluation raises no error
    val checked = new DeltaCheckingOptimizee(problem)
    val config = GeneticSearchConfig(crossoverEnabled = false)
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(checked, config, new Random(1), (_, _) => Seq.empty, added += _)
    val tour = problem.getInitialGuess
    val population = ArrayBuffer(ParameterArrayWithFitness(tour, problem.evaluateFitness(tour)))
    refiller.refill(population, 1, 30, tour, 0.1)
    assert(added.nonEmpty)
  }

  /** Opts in to moves and counts how often a neighbor is built, whichever way it is asked for. */
  private class BuildCounting(values: IndexedSeq[com.barrybecker4.optimization.parameter.types.Parameter], rnd: Random,
                              val built: java.util.concurrent.atomic.AtomicInteger)
    extends PermutedParameterArray(values, rnd) {
    override def supportsMoves: Boolean = true
    override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
      built.incrementAndGet()
      new BuildCounting(super.getRandomNeighbor(radius).params, rnd, built)
    }
    override def applyMove(move: Move): PermutedParameterArray = {
      built.incrementAndGet()
      new BuildCounting(super.applyMove(move).params, rnd, built)
    }
  }

  /** A tour that has had many improving swaps, so that most neighbors of it are worse and a greedy search has to retry. */
  private def improvedTour(problem: RandomTourProblem): PermutedParameterArray = {
    val rnd = new Random(1)
    var tour = problem.getInitialGuess.asInstanceOf[PermutedParameterArray]
    var length = problem.evaluateFitness(tour)
    for (_ <- 1 to 5000) {
      val move = Swap(rnd.nextInt(tour.size), rnd.nextInt(tour.size))
      val after = problem.evaluateMove(tour, length, move)
      if (after < length - 1e-12) { tour = tour.applyMove(move); length = after }
    }
    tour
  }

  private def buildsFor(useMoves: Boolean): (Int, Int) = {
    val problem = new RandomTourProblem(60, 7)
    val built = new java.util.concurrent.atomic.AtomicInteger(0)
    val tour = new BuildCounting(improvedTour(problem).params, new Random(3), built)
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(problem, GeneticSearchConfig(crossoverEnabled = false, useMoves = useMoves),
      new Random(3), (_, _) => Seq.empty, added += _)
    refiller.refill(ArrayBuffer(ParameterArrayWithFitness(tour, problem.evaluateFitness(tour))), 1, 30, tour, 0.1)
    (built.get, added.size)
  }

  test("with moves, a neighbor is built only for a mutant that comes back, which is far fewer than are tried") {
    val (builtWithMoves, kept) = buildsFor(useMoves = true)
    val (builtWithout, _) = buildsFor(useMoves = false)
    assert(kept > 0 && builtWithMoves >= kept)
    assert(builtWithMoves <= kept + 3, s"$builtWithMoves built for $kept kept (a few mutants repeat a tour already seen)")
    assert(builtWithout > 2 * builtWithMoves, s"$builtWithout built without moves, $builtWithMoves with")
  }

  test("without moves every try is a full evaluation") {
    val problem = new CountingTour(200, 7)
    mutants(problem, 200, useMoves = false, 1)
    assertResult(0)(problem.moveEvaluations)
    assert(problem.fullEvaluations > 10)
  }

  test("an array that picks its own neighbors keeps doing so, and its mutants keep its type") {
    val problem = new RandomTourProblem(30, 2)
    val path = new PuzzleLikePath(problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params, "route", new Random(2))
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(problem, GeneticSearchConfig(crossoverEnabled = false), new Random(2), (_, _) => Seq.empty, added += _)
    refiller.refill(ArrayBuffer(ParameterArrayWithFitness(path, problem.evaluateFitness(path))), 1, 20, path, 0.1)
    assert(path.neighborsAsked > 0 && added.nonEmpty)
    assert(added.forall(_.pa.isInstanceOf[PuzzleLikePath]))
  }

  test("moves are on by default") {
    assert(GeneticSearchConfig().useMoves)
  }
}

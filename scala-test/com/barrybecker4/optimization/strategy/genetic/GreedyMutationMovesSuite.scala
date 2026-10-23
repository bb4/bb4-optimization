// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.optimizee.DeltaCheckingOptimizee
import com.barrybecker4.optimization.parameter.{Move, ParameterArray, ParameterArrayWithFitness, PermutedParameterArray, PuzzleLikePath}
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

  test("with moves, neighbors that are not kept are never built or evaluated in full") {
    val problem = new CountingTour(200, 7)
    val added = mutants(problem, 200, useMoves = true, 1)
    assert(added.nonEmpty && problem.moveEvaluations > added.size, s"${problem.moveEvaluations} tries for ${added.size} mutants")
    assert(problem.fullEvaluations <= 3 + 0, s"${problem.fullEvaluations} full evaluations (3 are for the starting tours)")
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

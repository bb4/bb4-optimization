// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, PermutedParameterArray}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/** Genetic search crosses parents with the parents' own crossWith, so an array can cross itself its own way. */
class CrossoverHookSuite extends AnyFunSuite {

  /** Its children are always the first parent's tour, rotated by one, and it counts how often it is asked. */
  private class RotatingTour(values: IndexedSeq[Parameter], rnd: Random, val crossings: java.util.concurrent.atomic.AtomicInteger)
    extends PermutedParameterArray(values, rnd) {
    override def crossWith(other: ParameterArray, rnd: Random): ParameterArray = {
      crossings.incrementAndGet()
      new RotatingTour(values.tail :+ values.head, rnd, crossings)
    }
    override def getRandomNeighbor(radius: Double): PermutedParameterArray =
      new RotatingTour(super.getRandomNeighbor(radius).params, rnd, crossings)
  }

  test("the children of a crossover come from the parents' crossWith") {
    val problem = new RandomTourProblem(12, 3)
    val crossings = new java.util.concurrent.atomic.AtomicInteger(0)
    val base = problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params
    val parents = (1 to 3).map { k =>
      val tour = new RotatingTour(new Random(k).shuffle(base), new Random(k), crossings)
      ParameterArrayWithFitness(tour, problem.evaluateFitness(tour))
    }
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(problem, GeneticSearchConfig(crossoverProbability = 1.0), new Random(5), (cands, _) =>
      cands.map(pa => ParameterArrayWithFitness(pa, problem.evaluateFitness(pa))), added += _)
    refiller.refill(ArrayBuffer(parents*), 3, 12, parents.head.pa, 0.1)
    assert(crossings.get > 0, "the parents' own crossWith was used")
    assert(added.forall(_.pa.isInstanceOf[RotatingTour]))
  }

  test("parents that cannot be crossed are skipped, as before") {
    class Refusing(values: IndexedSeq[Parameter], rnd: Random) extends PermutedParameterArray(values, rnd) {
      override def crossWith(other: ParameterArray, rnd: Random): ParameterArray = throw new IllegalArgumentException("no")
    }
    val problem = new RandomTourProblem(10, 1)
    val params = problem.getInitialGuess.asInstanceOf[PermutedParameterArray].params
    val parents = (1 to 3).map { k =>
      val tour = new Refusing(new Random(k).shuffle(params), new Random(k))
      ParameterArrayWithFitness(tour, problem.evaluateFitness(tour))
    }
    val refiller = new PopulationRefiller(problem, GeneticSearchConfig(crossoverProbability = 1.0, neighborSelection = NeighborSelection.SingleSample),
      new Random(2), (cands, _) => cands.map(pa => ParameterArrayWithFitness(pa, problem.evaluateFitness(pa))), _ => ())
    val population = ArrayBuffer(parents*)
    refiller.refill(population, 3, 8, parents.head.pa, 0.1)
    assert(population.size >= 8, "mutation made up for the crossings that were refused")
  }
}

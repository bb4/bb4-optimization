// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.benchmark.RandomTourProblem
import com.barrybecker4.optimization.parameter.types.{IntegerParameter, Parameter}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness, PermutedParameterArray}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/** Whatever way a candidate is made (crossover or any kind of mutation), the problem gets to repair it first. */
class RepairHookSuite extends AnyFunSuite {

  /** A tour is only acceptable when it starts at city 0, so a problem that wants that repairs a tour that does not. */
  private class StartsAtCityZero(cities: Int, seed: Long) extends RandomTourProblem(cities, seed) {
    var repaired = 0
    override def repair(candidate: ParameterArray): ParameterArray = {
      val values = (0 until candidate.size).map(i => candidate.get(i).getValue.toInt)
      val at = values.indexOf(0)
      if (at == 0) candidate
      else {
        repaired += 1
        val order = values.drop(at) ++ values.take(at)
        PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, cities - 1, "p" + i): Parameter), new Random(at))
      }
    }
  }

  private def valuesOf(pa: ParameterArray): Seq[Int] = (0 until pa.size).map(i => pa.get(i).getValue.toInt)

  private def population(problem: RandomTourProblem, cities: Int, count: Int): ArrayBuffer[ParameterArrayWithFitness] =
    ArrayBuffer((1 to count).map { k =>
      val order = 0 +: new Random(k).shuffle(1 until cities)
      val tour = PermutedParameterArray(order.indices.map(i => IntegerParameter(order(i), 0, cities - 1, "p" + i): Parameter), new Random(k))
      ParameterArrayWithFitness(tour, problem.evaluateFitness(tour))
    }*)

  private def refill(problem: RandomTourProblem, config: GeneticSearchConfig, cities: Int): Seq[ParameterArrayWithFitness] = {
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(problem, config, new Random(3),
      (cands, _) => cands.map(pa => ParameterArrayWithFitness(pa, problem.evaluateFitness(pa))), added += _)
    val members = population(problem, cities, 3)
    refiller.refill(members, 3, 30, members.head.pa, 0.5)
    added.toSeq
  }

  private def check(config: GeneticSearchConfig, name: String): Unit = {
    val problem = new StartsAtCityZero(14, 2)
    val added = refill(problem, config, 14)
    assert(added.nonEmpty, name)
    assert(problem.repaired > 0, s"$name: some candidate needed repair")
    added.foreach { m =>
      assertResult(0, s"$name: every member starts at city 0")(valuesOf(m.pa).head)
      assert(Math.abs(m.fitness - problem.evaluateFitness(m.pa)) < 1e-9, s"$name: fitness is that of the repaired tour")
    }
  }

  test("crossover children are repaired")(check(GeneticSearchConfig(crossoverProbability = 1.0, neighborSelection = NeighborSelection.SingleSample), "crossover"))

  test("single-sample mutants are repaired")(check(GeneticSearchConfig(crossoverEnabled = false, neighborSelection = NeighborSelection.SingleSample), "single sample"))

  test("greedy mutants made with moves are repaired, and scored again if the repair changed them")(
    check(GeneticSearchConfig(crossoverEnabled = false, useMoves = true), "greedy with moves"))

  test("greedy mutants made by building neighbors are repaired")(
    check(GeneticSearchConfig(crossoverEnabled = false, useMoves = false), "greedy building"))

  test("a problem that needs no repair costs nothing and changes nothing") {
    val problem = new RandomTourProblem(14, 2)
    val withHook = refill(problem, GeneticSearchConfig(crossoverEnabled = false), 14).map(m => valuesOf(m.pa))
    assert(withHook.nonEmpty)
  }

  test("the initial population is repaired too") {
    val problem = new StartsAtCityZero(14, 2)
    val seed = population(problem, 14, 1).head.pa
    val neighbors = InitialPopulation.proposeNeighbors(seed, 20, 1.0, problem.repair)
    assert(neighbors.nonEmpty && neighbors.forall(valuesOf(_).head == 0))
  }
}

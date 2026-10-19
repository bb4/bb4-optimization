package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.optimizee.BudgetedOptimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.genetic.optimizees.RecordingOptimizee
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class PopulationRefillerSuite extends AnyFunSuite {

  private def keepers(optimizee: RecordingOptimizee) =
    ArrayBuffer(Seq((1.0, 2.0), (-1.0, 3.0), (0.5, -2.0)).map { case (a, b) =>
      val pa = NumericParameterArraySuiteLike.twoDoubleParams(a, b)
      ParameterArrayWithFitness(pa, optimizee.evaluateFitness(pa))
    }*)

  private final class Probe(config: GeneticSearchConfig, seed: Int = 1) {
    val optimizee = new RecordingOptimizee
    val batches = ArrayBuffer[Int]()
    val added = ArrayBuffer[ParameterArrayWithFitness]()
    val refiller = new PopulationRefiller(
      optimizee, config, new Random(seed),
      (cands: Seq[ParameterArray], ignored: ParameterArray) => {
        batches += cands.size
        cands.map(pa => ParameterArrayWithFitness(pa, optimizee.evaluateFitness(pa)))
      },
      added += _)
    val population: ArrayBuffer[ParameterArrayWithFitness] = keepers(optimizee)
    def refill(desired: Int): Unit = refiller.refill(population, 3, desired, population.head.pa, 0.1)
  }

  test("fills the population to the desired size and reports exactly the added members") {
    val p = new Probe(GeneticSearchConfig(neighborSelection = NeighborSelection.SingleSample))
    p.refill(desired = 15)
    assert(p.population.size == 15)
    assert(p.added.size == 12)
    assert(p.added.forall(a => p.population.exists(_ eq a)))
  }

  test("never exceeds the desired size even when a batch is larger than the room left") {
    for (seed <- 1 to 10) {
      val p = new Probe(GeneticSearchConfig(neighborSelection = NeighborSelection.SingleSample), seed)
      p.refill(desired = 8)
      assert(p.population.size <= 8)
    }
  }

  test("a population that is already full is left alone") {
    val p = new Probe(GeneticSearchConfig())
    p.refill(desired = 3)
    assert(p.population.size == 3 && p.added.isEmpty && p.batches.isEmpty)
  }

  test("single-sample mutation scores its proposals in a batch") {
    val p = new Probe(GeneticSearchConfig(crossoverEnabled = false, neighborSelection = NeighborSelection.SingleSample))
    p.refill(desired = 10)
    assert(p.batches.toSeq == Seq(7))
  }

  test("greedy mutation evaluates inline instead of using the batch scorer") {
    val p = new Probe(GeneticSearchConfig(crossoverEnabled = false, neighborSelection = NeighborSelection.GreedyImproveAbsolute))
    p.refill(desired = 10)
    assert(p.population.size == 10)
    assert(p.batches.isEmpty)
  }

  test("greedy mutation spends exactly maxNeighborRetries + 1 evaluations when nothing beats the parent") {
    val retries = 2
    val counting = new BudgetedOptimizee(new FlatLandscapeOptimizee, maxEvaluations = 1000)
    val config = GeneticSearchConfig(
      crossoverEnabled = false, neighborSelection = NeighborSelection.GreedyImproveAbsolute, maxNeighborRetries = retries)
    val refiller = new PopulationRefiller(counting, config, new Random(1), (_, _) => Seq.empty, _ => ())
    val pop = ArrayBuffer(ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(1.0, 2.0), 1.0))
    refiller.refill(pop, keepSize = 1, desiredSize = 2, baseline = pop.head.pa, nbrRadius = 0.1) // one mutant wanted
    assert(pop.size == 2)
    assert(counting.evaluationsUsed == retries + 1)
  }

  test("a single keeper cannot be crossed, so a crossover-happy config still fills by mutation") {
    val p = new Probe(GeneticSearchConfig(crossoverProbability = 1.0, neighborSelection = NeighborSelection.SingleSample))
    p.population.remove(1, 2)
    p.refiller.refill(p.population, keepSize = 1, desiredSize = 6, baseline = p.population.head.pa, nbrRadius = 0.1)
    assert(p.population.size == 6)
    val keys = p.population.map(m => Solutions.key(m.pa))
    assert(keys.distinct.size == keys.size)
  }
}

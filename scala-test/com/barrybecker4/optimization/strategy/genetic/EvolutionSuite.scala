package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.types.DoubleParameter
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.optimizees.FlatLandscapeOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class EvolutionSuite extends AnyFunSuite {

  private final class Quadratic(target: Double) extends Optimizee {
    override def getName: String = "q"
    override def evaluateByComparison: Boolean = false
    override def evaluateFitness(p: ParameterArray): Double = { val d = p.get(0).getValue - target; d * d }
    override def compareFitness(a: ParameterArray, b: ParameterArray): Double = throw new UnsupportedOperationException
    override def getOptimalFitness: Double = 0.0
  }

  /** A seeded evolution over a 1-D parameter, with an absolute-fitness optimizee. */
  private def evolution(optimizee: Optimizee, config: GeneticSearchConfig, startX: Double,
                        fitnessRange: Double = 100.0, desiredSize: Int = 12): (Evolution, ChampionTracker) = {
    val rnd = new Random(7)
    val seed = NumericParameterArray(IndexedSeq(DoubleParameter(startX, -10.0, 10.0, "x")), rnd = rnd)
    val tracker = new ChampionTracker(false, _ => ())
    val score = (cands: Seq[ParameterArray], ignored: ParameterArray) =>
      cands.map(pa => ParameterArrayWithFitness(pa, optimizee.evaluateFitness(pa)))
    val refiller = new PopulationRefiller(optimizee, config, rnd, score, m => tracker.considerMember(m))
    val seedMember = ParameterArrayWithFitness(seed, optimizee.evaluateFitness(seed))
    tracker.consider(seedMember)
    val population = ArrayBuffer(seedMember)
    score(InitialPopulation.proposeNeighbors(seed, desiredSize, config.initialNeighborRadius), seed).foreach { m =>
      population += m
      tracker.considerMember(m)
    }
    val rescore = (pop: ArrayBuffer[ParameterArrayWithFitness], base: ParameterArray) => ()
    (new Evolution(population, tracker, refiller, rescore, config, desiredSize, fitnessRange), tracker)
  }

  test("nothing stops a fresh evolution") {
    val (e, _) = evolution(new Quadratic(1.5), GeneticSearchConfig(), startX = -4.0)
    assert(e.stopReason(optimalReached = false).isEmpty)
    assert(e.generations == 0)
  }

  test("stops when the optimum is reached") {
    val (e, _) = evolution(new Quadratic(1.5), GeneticSearchConfig(), startX = -4.0)
    assert(e.stopReason(optimalReached = true).exists(_.contains("optimal")))
  }

  test("stops at the generation limit, not one generation later") {
    val (e, _) = evolution(new Quadratic(1.5), GeneticSearchConfig(maxIterations = 3, maxStagnantGenerations = 100), -4.0)
    for (_ <- 1 to 3) {
      assert(e.stopReason(false).isEmpty)
      e.nextGeneration()
    }
    assert(e.generations == 3)
    assert(e.stopReason(false).exists(_.contains("MAX ITERATIONS")))
  }

  test("a zero generation limit stops immediately") {
    val (e, _) = evolution(new Quadratic(1.5), GeneticSearchConfig(maxIterations = 0), -4.0)
    assert(e.stopReason(false).exists(_.contains("MAX ITERATIONS")))
  }

  test("stops after more than maxStagnantGenerations generations without progress") {
    val config = GeneticSearchConfig(improvementEpsilon = 0.0, maxStagnantGenerations = 2)
    val (e, _) = evolution(new FlatLandscapeOptimizee, config, startX = 2.0)
    var generations = 0
    while (e.stopReason(false).isEmpty) { e.nextGeneration(); generations += 1 }
    assert(generations == 3)
    assert(e.stopReason(false).exists(_.contains("IMPROVEMENT")))
  }

  test("reports a non-positive delta and a champion that never gets worse") {
    val (e, tracker) = evolution(new Quadratic(1.5), GeneticSearchConfig(), startX = -8.0)
    var previous = tracker.get.get.fitness
    for (i <- 0 until 6) {
      val report = e.nextGeneration()
      assert(report.index == i)
      assert(report.delta <= 0.0)
      assert(report.champion.fitness <= previous)
      assert(report.previousChampion.fitness == previous)
      assert(math.abs(report.delta - math.min(report.champion.fitness - previous, 0.0)) < 1e-12)
      previous = report.champion.fitness
    }
  }

  test("the radius shrinks while there is no progress") {
    val config = GeneticSearchConfig(nbrRadius = 0.5, nbrRadiusShrinkFactor = 0.5)
    val (e, _) = evolution(new FlatLandscapeOptimizee, config, startX = 2.0)
    val radii = (1 to 3).map(_ => e.nextGeneration().radius)
    assert(radii == Seq(0.5, 0.25, 0.125))
  }

  test("the radius grows after big improvements but never beyond the proposal limit") {
    // a tiny fitnessRange makes any improvement "big"
    val config = GeneticSearchConfig(nbrRadius = 1.0, nbrRadiusExpandFactor = 3.0)
    val (e, _) = evolution(new Quadratic(1.5), config, startX = -9.0, fitnessRange = 1e-9)
    val first = e.nextGeneration()
    assert(first.delta < 0.0, "the start is far from the optimum, so the first generation should improve")
    assert(e.nextGeneration().radius == 1.5)
  }
}

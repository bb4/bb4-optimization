package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer

class ChampionTrackerSuite extends AnyFunSuite {

  private def member(x: Double, fitness: Double) =
    ParameterArrayWithFitness(NumericParameterArraySuiteLike.twoDoubleParams(x, 0.0), fitness)

  test("absolute mode keeps the lowest fitness and reports every candidate") {
    val seen = ArrayBuffer[Double]()
    val tracker = new ChampionTracker(compareMode = false, m => seen += m.fitness)
    assert(tracker.get.isEmpty)
    Seq(5.0, 3.0, 4.0, 1.0, 2.0).foreach(f => tracker.consider(member(f, f)))
    assert(tracker.get.map(_.fitness).contains(1.0))
    assert(seen.toSeq == Seq(5.0, 3.0, 4.0, 1.0, 2.0))
  }

  test("the first candidate of an equal pair stays champion") {
    val tracker = new ChampionTracker(false, _ => ())
    val first = member(1.0, 2.0)
    tracker.consider(first)
    tracker.consider(member(9.0, 2.0))
    assert(tracker.get.exists(_ eq first))
  }

  test("orElse returns the fallback only when there is no champion") {
    val tracker = new ChampionTracker(false, _ => ())
    val fallback = member(0.0, 100.0)
    assert(tracker.orElse(fallback) eq fallback)
    tracker.consider(member(1.0, 1.0))
    assert(tracker.orElse(fallback).fitness == 1.0)
  }

  test("reset forgets the champion") {
    val tracker = new ChampionTracker(false, _ => ())
    tracker.consider(member(1.0, 1.0))
    tracker.reset()
    assert(tracker.get.isEmpty)
  }

  test("absolute mode does not translate fitness between frames") {
    val tracker = new ChampionTracker(false, _ => ())
    tracker.consider(member(1.0, -20.0))
    tracker.beginGeneration()
    val m = member(2.0, -3.0)
    assert(tracker.toChampionFrame(m) eq m)
    assert(tracker.toPopulationFrame(m) eq m)
  }

  test("compare mode adds improvements to the champion's cumulative fitness") {
    val tracker = new ChampionTracker(compareMode = true, _ => ())
    tracker.consider(member(1.0, -20.0)) // seeding: scored against the initial guess
    tracker.beginGeneration()            // the next generation is scored against this champion
    tracker.considerMember(member(2.0, -3.0))
    assert(tracker.get.exists(_.fitness == -23.0))
  }

  test("compare mode ignores a member that does not beat the baseline") {
    val tracker = new ChampionTracker(true, _ => ())
    val champion = member(1.0, -20.0)
    tracker.consider(champion)
    tracker.beginGeneration()
    tracker.considerMember(member(2.0, 0.0))
    tracker.considerMember(member(3.0, 4.0))
    assert(tracker.get.exists(_ eq champion))
  }

  test("compare mode translates the champion back into the population's frame") {
    val tracker = new ChampionTracker(true, _ => ())
    tracker.consider(member(1.0, -20.0))
    tracker.beginGeneration()
    tracker.considerMember(member(2.0, -3.0)) // champion is now -23, baseline still -20
    assert(tracker.toPopulationFrame(tracker.get.get).fitness == -3.0)
  }

  test("ensureInPopulation does nothing when the champion is already a member") {
    val tracker = new ChampionTracker(false, _ => ())
    val champion = member(0.0, 0.0)
    tracker.consider(champion)
    val pop = ArrayBuffer(member(5.0, 10.0), member(0.0, 0.0))
    tracker.ensureInPopulation(pop, desiredSize = 2)
    assert(pop.size == 2)
  }

  test("ensureInPopulation appends when there is room") {
    val tracker = new ChampionTracker(false, _ => ())
    tracker.consider(member(0.0, 0.0))
    val pop = ArrayBuffer(member(5.0, 10.0))
    tracker.ensureInPopulation(pop, desiredSize = 3)
    assert(pop.size == 2 && pop.exists(_.fitness == 0.0))
  }

  test("ensureInPopulation replaces the worst member when full") {
    val tracker = new ChampionTracker(false, _ => ())
    tracker.consider(member(0.0, 0.0))
    val pop = ArrayBuffer(member(5.0, 10.0), member(4.0, 8.0), member(3.0, 6.0))
    tracker.ensureInPopulation(pop, desiredSize = 3)
    assert(pop.map(_.fitness).sorted == Seq(0.0, 6.0, 8.0))
  }

  test("set installs a champion and reports it") {
    val seen = ArrayBuffer[Double]()
    val tracker = new ChampionTracker(false, m => seen += m.fitness)
    tracker.set(member(0.0, 7.0))
    assert(tracker.get.exists(_.fitness == 7.0) && seen.toSeq == Seq(7.0))
  }
}

package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStepMode.{AdaptiveAcceptance, Fixed}
import org.scalatest.funsuite.AnyFunSuite

class StepScalerSuite extends AnyFunSuite {

  private def record(s: StepScaler, outcomes: Boolean*): Unit = outcomes.foreach(s.record)

  test("Fixed mode never changes the scale") {
    val s = new StepScaler(Fixed)
    record(s, Seq.fill(500)(true)*)
    assert(s.scale == 1.0)
  }

  test("starts at initialScale") {
    assert(new StepScaler(AdaptiveAcceptance(initialScale = 2.5)).scale == 2.5)
  }

  test("scale grows when acceptance is above target and shrinks when below") {
    val mode = AdaptiveAcceptance(targetRate = 0.5, windowProposals = 4, growFactor = 2.0, shrinkFactor = 0.5)
    val s = new StepScaler(mode)
    record(s, true, true, true, false) // 0.75 > 0.5
    assert(s.scale == 2.0)
    record(s, false, false, false, true) // 0.25 < 0.5
    assert(s.scale == 1.0)
  }

  test("scale is unchanged when the window rate equals the target") {
    val s = new StepScaler(AdaptiveAcceptance(targetRate = 0.5, windowProposals = 4))
    record(s, true, false, true, false)
    assert(s.scale == 1.0)
  }

  test("nothing changes before a window completes") {
    val s = new StepScaler(AdaptiveAcceptance(windowProposals = 5, growFactor = 3.0))
    record(s, true, true, true, true)
    assert(s.scale == 1.0)
    record(s, true)
    assert(s.scale == 3.0)
  }

  test("each window is judged independently of the previous one") {
    val s = new StepScaler(AdaptiveAcceptance(targetRate = 0.5, windowProposals = 2, growFactor = 2.0, shrinkFactor = 0.5))
    record(s, true, true) // grow to 2
    record(s, true, false) // exactly on target: unchanged (would grow if the old counts leaked)
    assert(s.scale == 2.0)
  }

  test("scale is clamped to scaleMin and scaleMax") {
    val up = new StepScaler(AdaptiveAcceptance(windowProposals = 1, scaleMax = 3.0, growFactor = 10.0))
    record(up, true, true, true)
    assert(up.scale == 3.0)

    val down = new StepScaler(AdaptiveAcceptance(windowProposals = 1, scaleMin = 0.25, shrinkFactor = 0.1))
    record(down, false, false, false)
    assert(down.scale == 0.25)
  }

  test("non-positive windowProposals disables adaptation") {
    val s = new StepScaler(AdaptiveAcceptance(windowProposals = 0, initialScale = 1.5))
    record(s, Seq.fill(200)(true)*)
    assert(s.scale == 1.5)
  }

  test("the window is capped at half the proposals per level so short levels still adapt") {
    // default window is 50, but a level of 10 proposals (1 dimension) allows only 5
    val s = new StepScaler(AdaptiveAcceptance(growFactor = 2.0), proposalsPerLevel = 10)
    record(s, true, true, true, true)
    assert(s.scale == 1.0)
    record(s, true)
    assert(s.scale == 2.0)
  }

  test("the window cap never drops below one proposal") {
    val s = new StepScaler(AdaptiveAcceptance(growFactor = 2.0), proposalsPerLevel = 1)
    record(s, true)
    assert(s.scale == 2.0)
  }

  test("a long level leaves the configured window alone") {
    val s = new StepScaler(AdaptiveAcceptance(windowProposals = 8, growFactor = 2.0), proposalsPerLevel = 1000)
    record(s, Seq.fill(7)(true)*)
    assert(s.scale == 1.0)
    record(s, true)
    assert(s.scale == 2.0)
  }
}

// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.{LineGraphDemoProblem, SphereProblem}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType.{BRUTE_FORCE, STATE_SPACE_SEARCH}
import org.scalatest.funsuite.AnyFunSuite

/** Status text for a failed viewer run, and the background-run policy that used to swallow illegal arguments. */
class OptimizerEvalStatusSuite extends AnyFunSuite {

  test("a failed run reports the exception message") {
    val boom = new IllegalArgumentException(
      "STATE_SPACE_SEARCH requires an Optimizee that mixes in DiscreteStateSpace")
    val text = OptimizerEvalPanel.failureStatus(STATE_SPACE_SEARCH, boom)
    assert(text == s"Failed (STATE_SPACE_SEARCH): ${boom.getMessage}")
    assert(!text.contains("did not complete"))
  }

  test("a blank exception message falls back to the exception class") {
    assert(OptimizerEvalPanel.failureStatus(BRUTE_FORCE, new IllegalArgumentException()) ==
      "Failed (BRUTE_FORCE): IllegalArgumentException")
    assert(OptimizerEvalPanel.failureStatus(STATE_SPACE_SEARCH, new IllegalArgumentException("")) ==
      "Failed (STATE_SPACE_SEARCH): IllegalArgumentException")
    val blank = new IllegalArgumentException(null: String)
    assert(OptimizerEvalPanel.failureStatus(BRUTE_FORCE, blank) ==
      "Failed (BRUTE_FORCE): IllegalArgumentException")
  }

  test("IllegalArgumentException from a run is not turned into an empty result") {
    val boom = new IllegalArgumentException(
      "BRUTE_FORCE cannot enumerate a continuous parameter space")
    val caught = intercept[IllegalArgumentException] {
      OptimizerEvalPanel.backgroundResult(throw boom)
    }
    assert(caught.getMessage == boom.getMessage)
  }

  test("the strategies offered for a problem come from appliesTo") {
    val sphere = TopControls.strategiesFor(new SphereProblem).map(_.toString).toSeq
    assert(!sphere.contains("STATE_SPACE_SEARCH"))
    assert(!sphere.contains("BRUTE_FORCE"))
    assert(sphere.contains("DIFFERENTIAL_EVOLUTION"))

    val line = TopControls.strategiesFor(new LineGraphDemoProblem).map(_.toString).toSeq
    assert(line.contains("STATE_SPACE_SEARCH"))
    assert(line.contains("BRUTE_FORCE"))
    assert(line == OptimizationStrategyTypeValues)
  }

  private val OptimizationStrategyTypeValues = Seq(
    "GLOBAL_SAMPLING",
    "GLOBAL_HILL_CLIMBING",
    "GLOBAL_SIMULATED_ANNEALING",
    "HILL_CLIMBING",
    "SIMULATED_ANNEALING",
    "GENETIC_SEARCH",
    "CONCURRENT_GENETIC_SEARCH",
    "STATE_SPACE_SEARCH",
    "BRUTE_FORCE",
    "DIFFERENTIAL_EVOLUTION",
    "PARTICLE_SWARM",
    "ITERATED_LOCAL_SEARCH",
    "VARIABLE_NEIGHBORHOOD_SEARCH",
    "TABU_SEARCH",
    "MIN_CONFLICTS"
  )
}

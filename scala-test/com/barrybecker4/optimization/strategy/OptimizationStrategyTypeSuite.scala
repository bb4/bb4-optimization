// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.optimizee.optimizees.{DiscreteGrid2DDemoProblem, LineGraphDemoProblem, SphereProblem}
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, PermutedParameterArray, VariableLengthIntSet}
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, IntegerParameter}
import com.barrybecker4.optimization.strategy.statespace.optimizees.CompareOnlyOptimizee
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

/** Which strategies are legal for a problem shape. The viewer filters with this, it does not keep its own list. */
class OptimizationStrategyTypeSuite extends AnyFunSuite {

  private val sphere = new SphereProblem
  private val line = new LineGraphDemoProblem
  private val grid = new DiscreteGrid2DDemoProblem
  private val perm: PermutedParameterArray = PermutedParameterArray(
    IndexedSeq(new IntegerParameter(0, 0, 2, "a"), new IntegerParameter(1, 0, 2, "b")),
    new Random(1)
  )
  private val variableLength: VariableLengthIntSet = VariableLengthIntSet.createInstance(
    IndexedSeq(VariableLengthIntSet.createParam(1)),
    IndexedSeq(1, 2, 3),
    new Random(1)
  )

  test("state-space search applies only when the optimizee is a discrete state space") {
    assert(!STATE_SPACE_SEARCH.appliesTo(sphere, sphere.getInitialGuess))
    assert(STATE_SPACE_SEARCH.appliesTo(line, line.getInitialGuess))
    assert(STATE_SPACE_SEARCH.appliesTo(grid, grid.getInitialGuess))
    assert(OptimizationStrategyType.valuesForUi.contains(STATE_SPACE_SEARCH))
  }

  test("brute force applies to integer lattices and permutations, not continuous parameters") {
    assert(!BRUTE_FORCE.appliesTo(sphere, sphere.getInitialGuess))
    assert(BRUTE_FORCE.appliesTo(line, line.getInitialGuess))
    assert(BRUTE_FORCE.appliesTo(grid, grid.getInitialGuess))
    val mixed = NumericParameterArray(IndexedSeq(
      new IntegerParameter(1, 0, 5, "i"),
      new DoubleParameter(0.5, 0, 1, "x")
    ))
    assert(!BRUTE_FORCE.appliesTo(line, mixed))
    assert(BRUTE_FORCE.appliesTo(sphere, perm))
  }

  test("differential evolution and particle swarm need absolute fitness and numeric parameters") {
    assert(DIFFERENTIAL_EVOLUTION.appliesTo(sphere, sphere.getInitialGuess))
    assert(PARTICLE_SWARM.appliesTo(sphere, sphere.getInitialGuess))
    assert(DIFFERENTIAL_EVOLUTION.appliesTo(line, line.getInitialGuess))
    assert(!DIFFERENTIAL_EVOLUTION.appliesTo(sphere, perm))
    assert(!PARTICLE_SWARM.appliesTo(sphere, perm))
    val compare = new CompareOnlyOptimizee
    assert(!DIFFERENTIAL_EVOLUTION.appliesTo(compare, sphere.getInitialGuess))
    assert(!PARTICLE_SWARM.appliesTo(compare, sphere.getInitialGuess))
    assert(!STATE_SPACE_SEARCH.appliesTo(compare, sphere.getInitialGuess))
    assert(HILL_CLIMBING.appliesTo(compare, sphere.getInitialGuess))
  }

  test("hill climbing applies to numeric, permuted, and variable-length parameters") {
    assert(HILL_CLIMBING.appliesTo(sphere, sphere.getInitialGuess))
    assert(GLOBAL_HILL_CLIMBING.appliesTo(line, line.getInitialGuess))
    assert(HILL_CLIMBING.appliesTo(sphere, perm))
    assert(GLOBAL_HILL_CLIMBING.appliesTo(sphere, perm))
    assert(HILL_CLIMBING.appliesTo(sphere, variableLength))
    assert(GLOBAL_HILL_CLIMBING.appliesTo(sphere, variableLength))
  }

  test("continuous viewer problems keep every strategy except state-space search and brute force") {
    val guess = sphere.getInitialGuess
    val offered = OptimizationStrategyType.valuesForUi.filter(_.appliesTo(sphere, guess)).map(_.toString).toSet
    assert(!offered.contains("STATE_SPACE_SEARCH"))
    assert(!offered.contains("BRUTE_FORCE"))
    assert(offered == Set(
      "GLOBAL_SAMPLING",
      "GLOBAL_HILL_CLIMBING",
      "GLOBAL_SIMULATED_ANNEALING",
      "HILL_CLIMBING",
      "SIMULATED_ANNEALING",
      "GENETIC_SEARCH",
      "CONCURRENT_GENETIC_SEARCH",
      "DIFFERENTIAL_EVOLUTION",
      "PARTICLE_SWARM",
      "ITERATED_LOCAL_SEARCH",
      "VARIABLE_NEIGHBORHOOD_SEARCH",
      "TABU_SEARCH"
    ))
  }

  test("every catalog strategy applies to the integer line and the integer grid") {
    OptimizationStrategyType.valuesForUi.foreach { strategy =>
      assert(strategy.appliesTo(line, line.getInitialGuess), strategy.toString)
      assert(strategy.appliesTo(grid, grid.getInitialGuess), strategy.toString)
    }
  }
}


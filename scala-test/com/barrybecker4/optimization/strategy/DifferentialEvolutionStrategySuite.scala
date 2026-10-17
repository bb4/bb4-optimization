package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.optimizees.{AckleyProblem, SphereProblem}
import org.scalatest.funsuite.AnyFunSuite

class DifferentialEvolutionStrategySuite extends AnyFunSuite {

  test("Differential Evolution converges on convex Sphere problem") {
    val problem = SphereProblem(dimensions = 2)
    val strategy = DifferentialEvolutionStrategy(problem, populationSize = 30)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 1.0e-3,
      s"Expected fitness <= 1e-3, but got ${solution.fitness}")
  }

  test("Differential Evolution escapes local minima on Ackley function") {
    val problem = AckleyProblem(dimensions = 2)
    val strategy = DifferentialEvolutionStrategy(problem, populationSize = 40, fWeight = 0.7, crossoverRate = 0.8)

    val solution = strategy.doOptimization(problem.getInitialGuess, problem.getFitnessRange)

    assert(solution.fitness <= 0.05, s"DE should reach the near-zero basin on Ackley, got ${solution.fitness}")
  }

  test("population smaller than 4 is rejected instead of spinning") {
    val problem = SphereProblem(dimensions = 2)
    intercept[IllegalArgumentException] {
      DifferentialEvolutionStrategy(problem, populationSize = 3)
    }
  }
}

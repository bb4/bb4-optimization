package com.barrybecker4.optimization

import com.barrybecker4.optimization.optimizee.optimizees.ErrorTolerances
import com.barrybecker4.optimization.optimizee.optimizees.problems.SevenElevenProblem
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import SevenElevenProblemSuite.ERROR_TOLERANCES
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite
import com.barrybecker4.optimization.optimizee.OptimizerTestSuite._

object SevenElevenProblemSuite {
  /** default error tolerance. */
  private val TOL = 0.006
  /** the tolerances for each for the search strategies. */
  val ERROR_TOLERANCES = ErrorTolerances(0.1, TOL / 10.0, TOL / 3.0, TOL / 10.0, TOL, TOL / 3.0, TOL / 3.0, TOL)
}

class SevenElevenProblemSuite extends OptimizerTestSuite {
  override protected def doTest(optType: OptimizationStrategyType): Unit = {
    val problem = new SevenElevenProblem
    val optimizer = new Optimizer(problem, Some(LOG_FILE_HOME + "sevenEleven_optimization.txt"))
    val initialGuess = problem.getInitialGuess
    verifyTest(optType, problem, initialGuess, optimizer, problem.getFitnessRange,
      ERROR_TOLERANCES.getErrorTolerancePercent(optType), "Seven Eleven")
  }
}

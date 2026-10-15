// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness

/** How an optimization run finished. */
enum OptimizationStatus {
  /** Search finished under its normal stopping rules. */
  case Completed
  /** Absolute fitness reached [[com.barrybecker4.optimization.optimizee.Optimizee.getOptimalFitness]]. */
  case OptimalReached
  /** Evaluation budget was exhausted; [[OptimizationResult.best]] is best-so-far. */
  case BudgetExhausted
}

/**
  * Outcome of [[Optimizer.doOptimization]].
  *
  * @param best             best parameter set found (including when the budget is exhausted)
  * @param status           how the run ended
  * @param evaluationsUsed  objective evaluations consumed when a budget was configured; otherwise `None`
  */
case class OptimizationResult(
    best: ParameterArrayWithFitness,
    status: OptimizationStatus,
    evaluationsUsed: Option[Int] = None
)

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray

/**
  * Thrown when [[BudgetedOptimizee]] has reached its evaluation limit.
  * @param maxEvaluations the budget that was configured
  */
class EvaluationBudgetExceededException(val maxEvaluations: Int)
  extends RuntimeException(s"Evaluation budget exceeded: $maxEvaluations calls to evaluateFitness/compareFitness")

/**
  * Wraps an [[Optimizee]] and counts each call to `evaluateFitness` or `compareFitness` toward a fixed budget.
  */
class BudgetedOptimizee(delegate: Optimizee, val maxEvaluations: Int) extends Optimizee {

  private var count = 0

  /** Number of objective evaluations consumed so far. */
  def evaluationsUsed: Int = count

  override def getName: String = delegate.getName

  override def evaluateByComparison: Boolean = delegate.evaluateByComparison

  override def evaluateFitness(params: ParameterArray): Double = {
    bump()
    delegate.evaluateFitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double = {
    bump()
    delegate.compareFitness(params1, params2)
  }

  override def getOptimalFitness: Double = delegate.getOptimalFitness

  private def bump(): Unit = {
    if (count >= maxEvaluations) throw new EvaluationBudgetExceededException(maxEvaluations)
    count += 1
  }
}

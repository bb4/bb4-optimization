// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchStrategy

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

  // Guarded by `this`: concurrent strategies call the optimizee from several threads.
  private var count = 0
  private var budgetExhausted = false

  /** Number of objective evaluations consumed so far. */
  def evaluationsUsed: Int = synchronized(count)

  /** True after a call was rejected because the evaluation limit was reached. */
  def wasBudgetExhausted: Boolean = synchronized(budgetExhausted)

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

  private def bump(): Unit = synchronized {
    if (count >= maxEvaluations) {
      budgetExhausted = true
      throw new EvaluationBudgetExceededException(maxEvaluations)
    }
    count += 1
  }
}

object BudgetedOptimizee {

  /**
    * Wrap `delegate` with an evaluation budget. When `delegate` mixes in [[DiscreteStateSpace]],
    * the wrapper also does so (needed for [[StateSpaceSearchStrategy]]).
    */
  def wrap(delegate: Optimizee, maxEvaluations: Int): BudgetedOptimizee =
    delegate match {
      case d: DiscreteStateSpace => new BudgetedDiscreteOptimizee(d, maxEvaluations)
      case _ => new BudgetedOptimizee(delegate, maxEvaluations)
    }
}

/**
  * [[BudgetedOptimizee]] that preserves [[DiscreteStateSpace]] for strategies that require it.
  */
private[optimizee] final class BudgetedDiscreteOptimizee(
    discreteDelegate: Optimizee & DiscreteStateSpace,
    maxEvaluations: Int
) extends BudgetedOptimizee(discreteDelegate, maxEvaluations) with DiscreteStateSpace {

  override def successors(state: ParameterArray): Seq[ParameterArray] =
    discreteDelegate.successors(state)

  override def heuristic(state: ParameterArray): Double =
    discreteDelegate.heuristic(state)
}

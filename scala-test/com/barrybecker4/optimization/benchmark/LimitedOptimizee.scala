// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.ParameterArray

import java.util.concurrent.atomic.AtomicLong

object LimitedOptimizee {
  val FINISHED = "finished"
  val TIME_LIMIT = "time"
  val EVALUATION_LIMIT = "evals"
}

/**
  * Counts the evaluations of an optimizee and stops the search when either a time limit or an evaluation limit is
  * reached. It stops the search the same way [[com.barrybecker4.optimization.optimizee.BudgetedOptimizee]] does, by
  * throwing [[EvaluationBudgetExceededException]], so the strategies that handle that return their best so far.
  * @param delegate the problem being optimized
  * @param maxEvaluations stop after this many evaluations
  * @param deadlineNanos stop at this time (as in `System.nanoTime`)
  */
class LimitedOptimizee(delegate: Optimizee, maxEvaluations: Long, deadlineNanos: Long) extends Optimizee {
  import LimitedOptimizee.*

  private val count = new AtomicLong(0)
  @volatile private var reason = FINISHED

  def evaluations: Long = count.get

  /** Why the search ended: FINISHED if the strategy decided to stop by itself. */
  def stoppedBy: String = reason

  override def getName: String = delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def getOptimalFitness: Double = delegate.getOptimalFitness

  override def evaluateFitness(params: ParameterArray): Double = {
    checkLimits()
    delegate.evaluateFitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double = {
    checkLimits()
    delegate.compareFitness(params1, params2)
  }

  private def checkLimits(): Unit = {
    if (count.incrementAndGet() > maxEvaluations) stop(EVALUATION_LIMIT)
    if (System.nanoTime() > deadlineNanos) stop(TIME_LIMIT)
  }

  private def stop(why: String): Nothing = {
    reason = why
    throw new EvaluationBudgetExceededException(maxEvaluations.toInt)
  }
}

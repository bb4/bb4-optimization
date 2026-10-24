// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.{EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{Move, ParameterArray}

import java.util.concurrent.atomic.AtomicLong
import scala.util.Random

object LimitedOptimizee {
  val FINISHED = "finished"
  val TIME_LIMIT = "time"
  val EVALUATION_LIMIT = "evals"
  val SOLVED = "solved"
}

/**
  * Counts the evaluations of an optimizee and stops the search when either a time limit or an evaluation limit is
  * reached. It stops the search the same way [[com.barrybecker4.optimization.optimizee.BudgetedOptimizee]] does, by
  * throwing [[EvaluationBudgetExceededException]], so the strategies that handle that return their best so far.
  * @param delegate the problem being optimized
  * @param maxEvaluations stop after this many evaluations
  * @param deadlineNanos stop at this time (as in `System.nanoTime`)
  * @param stopWhenSolved stop at the next evaluation after one reached the optimal fitness, for a problem where
  *                       that is the goal and the strategy might otherwise carry on
  */
class LimitedOptimizee(delegate: Optimizee, maxEvaluations: Long, deadlineNanos: Long, stopWhenSolved: Boolean = false)
    extends Optimizee {
  import LimitedOptimizee.*

  private val count = new AtomicLong(0)
  @volatile private var reason = FINISHED
  @volatile private var solved = false

  def evaluations: Long = count.get

  /** True once an evaluation returned the optimal fitness of the problem. */
  def reachedOptimal: Boolean = solved

  /** Why the search ended: FINISHED if the strategy decided to stop by itself. */
  def stoppedBy: String = reason

  override def getName: String = delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def getOptimalFitness: Double = delegate.getOptimalFitness
  override def repair(candidate: ParameterArray): ParameterArray = delegate.repair(candidate)
  override def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] =
    delegate.proposeGuidedMove(current, radius, rnd)
  override def conflictedPositions(current: ParameterArray): Seq[Int] = delegate.conflictedPositions(current)

  override def evaluateFitness(params: ParameterArray): Double = {
    checkLimits()
    noteIfSolved(delegate.evaluateFitness(params))
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double = {
    checkLimits()
    delegate.compareFitness(params1, params2)
  }

  /** A move evaluation counts as one evaluation, the same as a full one. */
  override def evaluateMove(current: ParameterArray, currentFitness: Double, move: Move): Double = {
    checkLimits()
    noteIfSolved(delegate.evaluateMove(current, currentFitness, move))
  }

  private def noteIfSolved(fitness: Double): Double = {
    if (fitness <= delegate.getOptimalFitness) solved = true
    fitness
  }

  private def checkLimits(): Unit = {
    if (count.incrementAndGet() > maxEvaluations) stop(EVALUATION_LIMIT)
    if (System.nanoTime() > deadlineNanos) stop(TIME_LIMIT)
    if (stopWhenSolved && solved) stop(SOLVED)
  }

  private def stop(why: String): Nothing = {
    reason = why
    throw new EvaluationBudgetExceededException(maxEvaluations.toInt)
  }
}

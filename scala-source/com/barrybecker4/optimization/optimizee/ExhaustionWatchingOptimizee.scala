// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{Move, ParameterArray}

import scala.util.Random

/**
  * Passes every call to `delegate` and notes when the delegate threw [[EvaluationBudgetExceededException]].
  * A strategy that catches that exception returns its best so far, so the caller could not tell otherwise
  * that a budget further out (not the one it set itself) is used up.
  */
class ExhaustionWatchingOptimizee(delegate: Optimizee) extends Optimizee {

  @volatile private var exhausted = false

  /** True once a call to the delegate was rejected because its evaluation limit was reached. */
  def sawExhaustion: Boolean = exhausted

  override def getName: String = delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def getOptimalFitness: Double = delegate.getOptimalFitness
  override def repair(candidate: ParameterArray): ParameterArray = delegate.repair(candidate)
  override def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] =
    delegate.proposeGuidedMove(current, radius, rnd)
  override def conflictedPositions(current: ParameterArray): Seq[Int] = delegate.conflictedPositions(current)

  override def evaluateFitness(params: ParameterArray): Double = watching(delegate.evaluateFitness(params))

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    watching(delegate.compareFitness(params1, params2))

  override def evaluateMove(current: ParameterArray, currentFitness: Double, move: Move): Double =
    watching(delegate.evaluateMove(current, currentFitness, move))

  private def watching(call: => Double): Double =
    try call
    catch {
      case e: EvaluationBudgetExceededException =>
        exhausted = true
        throw e
    }
}

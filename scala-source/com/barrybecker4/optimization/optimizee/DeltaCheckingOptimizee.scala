// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{Move, ParameterArray}

import scala.util.Random

/**
  * Wraps an optimizee and checks every [[evaluateMove]] against a full evaluation of the array after the move. A wrong
  * delta would silently steer a search the wrong way, so use this in tests, and while writing a delta, but not in
  * production: it does the full evaluations that the delta exists to avoid.
  *
  * It also checks that the fitness the strategy passes in for the current array is the true one.
  * Any mismatch throws an `IllegalStateException` that names the move.
  * @param delegate the optimizee with the delta to check
  * @param tolerance how far apart, relative to the size of the fitness, two values may be and still count as equal
  */
class DeltaCheckingOptimizee(delegate: Optimizee, tolerance: Double = 1e-9) extends Optimizee {

  override def getName: String = delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def evaluateFitness(params: ParameterArray): Double = delegate.evaluateFitness(params)
  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    delegate.compareFitness(params1, params2)
  override def getOptimalFitness: Double = delegate.getOptimalFitness
  override def repair(candidate: ParameterArray): ParameterArray = delegate.repair(candidate)
  override def proposeGuidedMove(current: ParameterArray, radius: Double, rnd: Random): Option[Move] =
    delegate.proposeGuidedMove(current, radius, rnd)
  override def conflictedPositions(current: ParameterArray): Seq[Int] = delegate.conflictedPositions(current)

  override def evaluateMove(current: ParameterArray, currentFitness: Double, move: Move): Double = {
    checkCurrentFitness(current, currentFitness, move)
    val viaDelta = delegate.evaluateMove(current, currentFitness, move)
    val viaFullEvaluation = delegate.evaluateFitness(current.applyMove(move))
    if (!isClose(viaDelta, viaFullEvaluation))
      throw new IllegalStateException(s"The fitness after $move was $viaDelta from evaluateMove, " +
        s"but $viaFullEvaluation from a full evaluation (off by ${viaDelta - viaFullEvaluation})")
    viaDelta
  }

  private def checkCurrentFitness(current: ParameterArray, claimed: Double, move: Move): Unit = {
    val actual = delegate.evaluateFitness(current)
    if (!isClose(claimed, actual))
      throw new IllegalStateException(s"The fitness given for the current array before $move was $claimed, but it is $actual")
  }

  private def isClose(a: Double, b: Double): Boolean =
    a == b || math.abs(a - b) <= tolerance * (1.0 + math.max(math.abs(a), math.abs(b)))
}

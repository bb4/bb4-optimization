// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.annealing

import com.barrybecker4.optimization.strategy.annealing.SimulatedAnnealingStepMode.{AdaptiveAcceptance, Fixed}

/**
  * Tracks the multiplicative scale applied to the base proposal radius of simulated annealing.
  * One instance is used per temperature level; it is not thread safe.
  *
  * With [[Fixed]] (or an [[AdaptiveAcceptance]] whose `windowProposals <= 0`) the scale never changes.
  * Otherwise, after every `windowProposals` proposals the observed acceptance rate is compared with the target:
  * above target grows the scale (steps too small), below target shrinks it (steps too large).
  */
private[annealing] final class StepScaler(mode: SimulatedAnnealingStepMode) {

  /** Differences smaller than this between the observed and target rate are treated as "on target". */
  private val RateTolerance = 1e-12

  private var currentScale: Double = mode match {
    case Fixed                    => 1.0
    case a: AdaptiveAcceptance    => a.initialScale
  }
  private var proposals = 0
  private var accepts = 0

  def scale: Double = currentScale

  /** Record the outcome of one proposal, adjusting the scale when a window completes. */
  def record(accepted: Boolean): Unit = mode match {
    case a: AdaptiveAcceptance if a.windowProposals > 0 =>
      proposals += 1
      if (accepted) accepts += 1
      if (proposals >= a.windowProposals) completeWindow(a)
    case _ => ()
  }

  private def completeWindow(a: AdaptiveAcceptance): Unit = {
    val rate = accepts.toDouble / proposals
    if (rate > a.targetRate + RateTolerance)
      currentScale = math.min(a.scaleMax, currentScale * a.growFactor)
    else if (rate < a.targetRate - RateTolerance)
      currentScale = math.max(a.scaleMin, currentScale * a.shrinkFactor)
    proposals = 0
    accepts = 0
  }
}

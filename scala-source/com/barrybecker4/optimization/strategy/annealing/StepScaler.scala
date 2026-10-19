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
  *
  * The window is capped at half of `proposalsPerLevel`, so the scale gets at least two adjustments per temperature
  * level. Without the cap the default window (50) is longer than a whole level when there are fewer than 5
  * dimensions (10 proposals per dimension), and the adaptation would never fire.
  *
  * @param proposalsPerLevel number of proposals made at one temperature level
  */
private[annealing] final class StepScaler(mode: SimulatedAnnealingStepMode, proposalsPerLevel: Int = Int.MaxValue) {

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
      if (proposals >= effectiveWindow(a)) completeWindow(a)
    case _ => ()
  }

  private def effectiveWindow(a: AdaptiveAcceptance): Int =
    math.min(a.windowProposals, math.max(1, proposalsPerLevel / 2))

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

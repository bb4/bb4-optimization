// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

/**
 * Decides whether the local optimum reached after a shake replaces the current solution in
 * [[IteratedLocalSearchStrategy]]. A strictly better local optimum is always accepted.
 */
enum LocalSearchAcceptance {

  /** Only move to a strictly better local optimum (the basic variable neighborhood search rule). */
  case Better

  /** Also move to an equally good local optimum, which lets the search drift across plateaus. */
  case BetterOrEqual

  /**
   * Also move to a worse local optimum with probability `exp(-delta / (relativeTemperature * fitnessRange))`,
   * where `delta` is how much worse it is and `fitnessRange` is the one passed to `doOptimization`.
   * Lets the search leave a deep basin, at the risk of wandering; the best solution seen is always kept.
   *
   * @param relativeTemperature temperature as a fraction of the fitness range; must be > 0 (e.g. 0.01)
   */
  case Metropolis(relativeTemperature: Double)
}

/**
 * Tunables for [[IteratedLocalSearchStrategy]].
 *
 * The search repeats: shake the current solution, run a local search from the shaken solution, and decide whether to
 * move to the local optimum that was found. How hard to shake is the "shake level" `k`: a level-`k` shake applies `k`
 * successive random neighbor moves, so higher levels reach further away. A fixed level (`minShakeLevel ==
 * maxShakeLevel`) is iterated local search; raising the level after every shake that fails to improve the current
 * solution (and resetting it after an improvement) is variable neighborhood search.
 *
 * @param maxIterations                  number of shake + local search rounds (not counting the first local search).
 * @param localRadius                    radius passed to `getRandomNeighbor` for every local move, in (0, 1.5].
 *                                       Its meaning depends on the parameter array: for permutations about
 *                                       `localRadius * size / 10` swaps (at least 1) per move; for variable length
 *                                       integer sets it is also the chance of adding or removing a member.
 * @param maxStaleNeighbors              local search stops, declaring a local optimum, after this many consecutive
 *                                       random neighbors that did not improve (including neighbors seen before).
 * @param minShakeLevel                  the shake level after an improvement, and the lowest level used; at least 1.
 * @param maxShakeLevel                  the highest shake level; after a failure at this level the level starts over
 *                                       at `minShakeLevel`.
 * @param acceptance                     whether to move to a local optimum that is not strictly better.
 * @param restartAfterStagnantIterations after this many rounds without a new best solution, jump to a random
 *                                       solution and run a local search from there (the best is kept); 0 disables.
 */
final case class IteratedLocalSearchConfig(
                                            maxIterations: Int = 200,
                                            localRadius: Double = 0.5,
                                            maxStaleNeighbors: Int = 100,
                                            minShakeLevel: Int = 3,
                                            maxShakeLevel: Int = 3,
                                            acceptance: LocalSearchAcceptance = LocalSearchAcceptance.BetterOrEqual,
                                            restartAfterStagnantIterations: Int = 50
                                          ) {
  require(maxIterations >= 0, s"maxIterations must not be negative, got $maxIterations")
  require(localRadius > 0.0 && localRadius <= 1.5,
    s"localRadius must be in (0, 1.5] (numeric neighbors reject larger radii), got $localRadius")
  require(maxStaleNeighbors >= 1, s"maxStaleNeighbors must be at least 1, got $maxStaleNeighbors")
  require(minShakeLevel >= 1, s"minShakeLevel must be at least 1, got $minShakeLevel")
  require(maxShakeLevel >= minShakeLevel,
    s"maxShakeLevel ($maxShakeLevel) must not be less than minShakeLevel ($minShakeLevel)")
  require(restartAfterStagnantIterations >= 0,
    s"restartAfterStagnantIterations must not be negative, got $restartAfterStagnantIterations")
  acceptance match {
    case LocalSearchAcceptance.Metropolis(relativeTemperature) =>
      require(relativeTemperature > 0.0 && relativeTemperature.isFinite,
        s"relativeTemperature must be finite and > 0, got $relativeTemperature")
    case _ => ()
  }
}

object IteratedLocalSearchConfig {

  /** Iterated local search: always shake with the same strength and accept equally good local optima. */
  val IteratedLocalSearch: IteratedLocalSearchConfig = IteratedLocalSearchConfig()

  /** Variable neighborhood search: escalate the shake from level 1 to 5, and only move to strictly better optima. */
  val VariableNeighborhoodSearch: IteratedLocalSearchConfig = IteratedLocalSearchConfig(
    minShakeLevel = 1,
    maxShakeLevel = 5,
    acceptance = LocalSearchAcceptance.Better
  )

  val Default: IteratedLocalSearchConfig = IteratedLocalSearch
}

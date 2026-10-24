// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.tabu

/**
  * Tunables for [[TabuSearchStrategy]].
  *
  * Every iteration looks at `candidateMoves` moves from the current solution and takes the best one that is not
  * tabu, even when it is worse than where the search is now. Undoing a move is forbidden for a number of iterations
  * (the tenure), which is what keeps the search from walking back into the local optimum it just left.
  *
  * @param maxIterations                  number of moves taken.
  * @param candidateMoves                 moves looked at (and scored) per iteration. All are scored, so this is also
  *                                       the number of evaluations per iteration.
  * @param tenureMin                      fewest iterations that undoing a move is forbidden.
  * @param tenureMax                      most iterations; the tenure of each move is drawn from `tenureMin` to
  *                                       `tenureMax`, which avoids cycles of one fixed length.
  * @param aspiration                     allow a tabu move when it gives a better solution than any seen so far.
  * @param restartAfterStagnantIterations after this many iterations without a new best solution, jump to a random
  *                                       solution and forget the tabu list (the best is kept); 0 disables.
  * @param fallbackRadius                 radius of the random neighbors used when the array has no local moves.
  */
final case class TabuSearchConfig(
    maxIterations: Int = 10000,
    candidateMoves: Int = 50,
    tenureMin: Int = 5,
    tenureMax: Int = 15,
    aspiration: Boolean = true,
    restartAfterStagnantIterations: Int = 500,
    fallbackRadius: Double = 0.5
) {
  require(maxIterations >= 0, s"maxIterations must not be negative, got $maxIterations")
  require(candidateMoves >= 1, s"candidateMoves must be at least 1, got $candidateMoves")
  require(tenureMin >= 0, s"tenureMin must not be negative, got $tenureMin")
  require(tenureMax >= tenureMin, s"tenureMax ($tenureMax) must not be less than tenureMin ($tenureMin)")
  require(restartAfterStagnantIterations >= 0,
    s"restartAfterStagnantIterations must not be negative, got $restartAfterStagnantIterations")
  require(fallbackRadius > 0.0 && fallbackRadius <= 1.5, s"fallbackRadius must be in (0, 1.5], got $fallbackRadius")
}

object TabuSearchConfig {
  val Default: TabuSearchConfig = TabuSearchConfig()
}

// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.minconflicts

/**
  * Tunables for [[MinConflictsStrategy]].
  *
  * Every step picks one position that is in conflict, looks at every move of that position, and takes the move that
  * leaves the fewest conflicts.
  *
  * @param maxSteps                    number of moves taken.
  * @param noiseProbability            chance that a step takes a random move of the position instead of the best one.
  *                                    A little noise (0.02 to 0.1) lets the search leave a local optimum.
  * @param randomTieBreaking           choose at random among moves that are equally good; if false the first is taken.
  * @param restartAfterStagnantSteps   after this many steps without a new best solution, jump to a random solution (the
  *                                    best is kept); 0 disables.
  */
final case class MinConflictsConfig(
    maxSteps: Int = 100000,
    noiseProbability: Double = 0.02,
    randomTieBreaking: Boolean = true,
    restartAfterStagnantSteps: Int = 2000
) {
  require(maxSteps >= 0, s"maxSteps must not be negative, got $maxSteps")
  require(noiseProbability >= 0.0 && noiseProbability <= 1.0, s"noiseProbability must be in [0, 1], got $noiseProbability")
  require(restartAfterStagnantSteps >= 0, s"restartAfterStagnantSteps must not be negative, got $restartAfterStagnantSteps")
}

object MinConflictsConfig {
  val Default: MinConflictsConfig = MinConflictsConfig()
}

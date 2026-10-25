// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.restart

/**
  * Tunables for [[RestartingStrategy]].
  *
  * Run number `i` (counting from 1) may use `luby(i) * baseBudget` evaluations, where `luby` is the sequence
  * 1 1 2 1 1 2 4 1 1 2 1 1 2 4 8 ... that is known to be within a log factor of the best fixed restart schedule
  * when the best budget is not known.
  *
  * @param maxRestarts                maximum number of runs of the inner strategy.
  * @param baseBudget                 evaluations in a run of Luby length 1.
  * @param totalBudget                evaluations across all the runs.
  * @param adaptBaseBudget            when the first run ends by itself inside its budget, use the evaluations it took as
  *                                   the base budget from then on, so the Luby lengths count natural runs of the strategy.
  * @param perturbRadius              radius of the neighbor taken from the best solution when a restart is a perturbation.
  * @param perturbationStep           added to the chance that a restart perturbs the best solution, for each restart
  *                                   in a row that did not beat it (fresh random starts that fail suggest the best is close).
  * @param maxPerturbationProbability limit of that chance; the rest of the restarts start from a random sample.
  */
final case class RestartingConfig(
    maxRestarts: Int = 1000,
    baseBudget: Int = 2000,
    totalBudget: Int = Int.MaxValue,
    adaptBaseBudget: Boolean = true,
    perturbRadius: Double = 1.0,
    perturbationStep: Double = 0.1,
    maxPerturbationProbability: Double = 0.5
) {
  require(maxRestarts >= 1, s"maxRestarts must be at least 1, got $maxRestarts")
  require(baseBudget >= 1, s"baseBudget must be at least 1, got $baseBudget")
  require(totalBudget >= 1, s"totalBudget must be at least 1, got $totalBudget")
  require(perturbRadius > 0.0 && perturbRadius <= 1.5, s"perturbRadius must be in (0, 1.5], got $perturbRadius")
  require(perturbationStep >= 0.0, s"perturbationStep must not be negative, got $perturbationStep")
  require(maxPerturbationProbability >= 0.0 && maxPerturbationProbability <= 1.0,
    s"maxPerturbationProbability must be in [0, 1], got $maxPerturbationProbability")
}

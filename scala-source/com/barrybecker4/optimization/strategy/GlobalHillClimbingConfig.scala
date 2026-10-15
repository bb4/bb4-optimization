// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

/** How multistart seeds are chosen from global samples. */
enum SeedSelection {

  /** Keep the `k` lowest-fitness global samples (legacy behavior). */
  case TopFitness

  /**
    * Keep the best `topM` samples by fitness, then greedily pick `k` diverse seeds
    * (max-min distance in parameter space).
    *
    * @param topM          fitness reservoir size before diversity selection
    * @param minSeparation minimum normalized distance between seeds (0–1 scale via typical random distance)
    */
  case DiverseFromTopM(topM: Int, minSeparation: Double)
}

object SeedSelection {

  /** Default diverse selection for [[GlobalHillClimbingConfig.multistartCount]]. */
  def defaultDiverseForMultistart(multistartCount: Int): SeedSelection =
    DiverseFromTopM(math.max(10 * multistartCount, 50), 0.15)
}

/**
  * Tunables for [[GlobalHillClimbingStrategy]].
  *
  * @param multistartCount    number of local hill climbs from global seeds
  * @param globalSampleBudget passed to [[GlobalSampleStrategy.setSamplingRate]]
  * @param seedSelection      how to pick multistart seeds from global samples
  */
case class GlobalHillClimbingConfig(
    multistartCount: Int = GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT,
    globalSampleBudget: Int = GlobalHillClimbingStrategy.DEFAULT_GLOBAL_SAMPLE_BUDGET,
    seedSelection: SeedSelection = SeedSelection.defaultDiverseForMultistart(
      GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT
    )
) {

  /** [[SeedSelection.DiverseFromTopM.topM]] uses `max(10 * multistartCount, 50)` when `topM <= 0`. */
  def effectiveSeedSelection: SeedSelection =
    seedSelection match {
      case SeedSelection.TopFitness =>
        SeedSelection.TopFitness
      case SeedSelection.DiverseFromTopM(topM, minSeparation) =>
        val m = if (topM > 0) topM else math.max(10 * multistartCount, 50)
        SeedSelection.DiverseFromTopM(m, minSeparation)
    }
}

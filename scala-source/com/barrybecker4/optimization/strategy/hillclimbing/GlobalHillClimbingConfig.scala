// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

/**
  * Tunables for [[GlobalHillClimbingStrategy]].
  *
  * @param multistartCount    number of local hill climbs from global seeds (at least 1)
  * @param globalSampleBudget (positive) passed to [[GlobalSampleStrategy.setSamplingRate]]
  * @param seedSelection      how to pick multistart seeds from global samples
  */
case class GlobalHillClimbingConfig(
    multistartCount: Int = GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT,
    globalSampleBudget: Int = GlobalHillClimbingStrategy.DEFAULT_GLOBAL_SAMPLE_BUDGET,
    seedSelection: SeedSelection = SeedSelection.defaultDiverseForMultistart(
      GlobalHillClimbingStrategy.DEFAULT_MULTISTART_COUNT
    ),
    useMoves: Boolean = false
) {

  require(multistartCount >= 1, s"multistartCount must be at least 1, got $multistartCount")
  require(globalSampleBudget > 0, s"globalSampleBudget must be positive, got $globalSampleBudget")
  seedSelection match {
    case SeedSelection.DiverseFromTopM(_, minSeparation) =>
      require(minSeparation >= 0.0, s"minSeparation must not be negative, got $minSeparation")
    case SeedSelection.TopFitness => ()
  }

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

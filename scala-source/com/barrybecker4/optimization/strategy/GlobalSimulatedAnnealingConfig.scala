// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

/**
  * Tunables for [[GlobalSimulatedAnnealingStrategy]].
  *
  * @param multistartCount       number of local SA runs from global seeds
  * @param globalSampleBudget    passed to [[GlobalSampleStrategy.setSamplingRate]]
  * @param seedSelection         how to pick multistart seeds from global samples
  * @param annealingConfig       config applied to each local [[SimulatedAnnealingStrategy]]
  */
case class GlobalSimulatedAnnealingConfig(
    multistartCount: Int = GlobalSimulatedAnnealingStrategy.DEFAULT_MULTISTART_COUNT,
    globalSampleBudget: Int = GlobalSimulatedAnnealingStrategy.DEFAULT_GLOBAL_SAMPLE_BUDGET,
    seedSelection: SeedSelection = SeedSelection.defaultDiverseForMultistart(
      GlobalSimulatedAnnealingStrategy.DEFAULT_MULTISTART_COUNT
    ),
    annealingConfig: SimulatedAnnealingConfig = SimulatedAnnealingConfig()
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

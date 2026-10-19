// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

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

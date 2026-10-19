// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

/**
 * Tunables for [[ParticleSwarmStrategy]].
 * The default weights are the well-known constriction-coefficient values of Clerc and Kennedy
 * (chi = 0.7298, c1 = c2 = 2.05 * chi), which keep the swarm stable without needing a large velocity clamp.
 *
 * @param swarmSize               number of particles. Larger swarms explore more but use more evaluations per iteration.
 * @param maxIterations           hard limit on the number of swarm updates.
 * @param inertiaWeight           fraction of a particle's previous velocity that is kept (w). Higher explores more.
 * @param cognitiveWeight         pull toward the particle's own best position (c1).
 * @param socialWeight            pull toward the best position in the particle's neighborhood (c2).
 * @param maxVelocityFraction     per-dimension speed limit, as a fraction of that parameter's range.
 * @param initialVelocityFraction initial speeds are uniform in +/- this fraction of each parameter's range.
 * @param topology                how particles share information (see [[ParticleSwarmTopology]]).
 * @param stagnationIterations    stop after this many consecutive iterations without a meaningful improvement
 *                                of the best fitness; if <= 0, only `maxIterations` (or reaching the
 *                                optimal fitness) ends the run.
 * @param stagnationTolerance     an iteration only counts as an improvement if the best fitness dropped by more than
 *                                `stagnationTolerance * fitnessRange` (the `fitnessRange` passed to `doOptimization`).
 */
final case class ParticleSwarmConfig(
                                      swarmSize: Int = 30,
                                      maxIterations: Int = 300,
                                      inertiaWeight: Double = 0.7298,
                                      cognitiveWeight: Double = 1.49618,
                                      socialWeight: Double = 1.49618,
                                      maxVelocityFraction: Double = 0.5,
                                      initialVelocityFraction: Double = 0.1,
                                      topology: ParticleSwarmTopology = ParticleSwarmTopology.Global,
                                      stagnationIterations: Int = 100,
                                      stagnationTolerance: Double = 1.0e-10
                                    ) {
  require(swarmSize >= 1, s"swarmSize must be at least 1, got $swarmSize")
  require(maxIterations >= 0, s"maxIterations must not be negative, got $maxIterations")
  require(inertiaWeight >= 0.0 && inertiaWeight.isFinite, s"inertiaWeight must be finite and >= 0, got $inertiaWeight")
  require(cognitiveWeight >= 0.0 && cognitiveWeight.isFinite,
    s"cognitiveWeight must be finite and >= 0, got $cognitiveWeight")
  require(socialWeight >= 0.0 && socialWeight.isFinite, s"socialWeight must be finite and >= 0, got $socialWeight")
  require(maxVelocityFraction > 0.0 && maxVelocityFraction.isFinite,
    s"maxVelocityFraction must be finite and > 0, got $maxVelocityFraction")
  require(initialVelocityFraction >= 0.0 && initialVelocityFraction.isFinite,
    s"initialVelocityFraction must be finite and >= 0, got $initialVelocityFraction")
  require(stagnationTolerance >= 0.0 && stagnationTolerance.isFinite,
    s"stagnationTolerance must be finite and >= 0, got $stagnationTolerance")
}

object ParticleSwarmConfig {
  val Default: ParticleSwarmConfig = ParticleSwarmConfig()
}

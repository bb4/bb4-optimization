// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.particleswarm

/**
 * Which particles each particle learns from (its "social" attractor) in [[ParticleSwarmStrategy]].
 */
enum ParticleSwarmTopology {

  /** Every particle is attracted to the best position found by the whole swarm. Converges fast, but is more
   * likely to collapse onto a local optimum of a multimodal landscape. */
  case Global

  /** Each particle is attracted to the best position found by itself and its two ring neighbors (index - 1, index + 1).
   * Information spreads slowly around the ring, which preserves diversity longer and is more robust on
   * multimodal problems at the cost of slower convergence. */
  case Ring
}

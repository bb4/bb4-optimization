// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.statespace

/** How the frontier priority queue is ordered. */
enum StateSpaceSearchMode {

  /** Expand lowest absolute (or compare-mode) fitness first (legacy behavior). */
  case BestFirstFitness

  /**
    * Expand the lowest `fitness + heuristic(state)` first.
    * Requires an admissible [[com.barrybecker4.optimization.optimizee.DiscreteStateSpace.heuristic]]
    * for correctness guarantees; ignored (falls back to [[BestFirstFitness]]) in compare mode.
    */
  case AStar
}

/**
  * Tunables for [[StateSpaceSearchStrategy]].
  *
  * @param maxExpansions upper bound on expanded nodes (enqueue successors for)
  * @param searchMode    frontier ordering policy
  */
case class StateSpaceSearchConfig(
    maxExpansions: Int = StateSpaceSearchConfig.DEFAULT_MAX_EXPANSIONS,
    searchMode: StateSpaceSearchMode = StateSpaceSearchMode.BestFirstFitness
)

object StateSpaceSearchConfig {
  /** Default cap on expanded nodes to avoid runaway memory on large graphs. */
  val DEFAULT_MAX_EXPANSIONS: Int = 100000

  val default: StateSpaceSearchConfig = StateSpaceSearchConfig()
}

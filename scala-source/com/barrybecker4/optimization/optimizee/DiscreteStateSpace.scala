// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.strategy.statespace.{StateSpaceSearchMode, StateSpaceSearchStrategy}

/**
  * Mix in with an [[Optimizee]] when the search space is a discrete graph, and you can enumerate
  * one-step transitions. Used by [[com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchStrategy]].
  * Successor lists should stay small; this API is for structured, low-branching problems—not huge graphs.
  */
trait DiscreteStateSpace {

  /** States reachable in one step from `state` (no duplicates required). */
  def successors(state: ParameterArray): Seq[ParameterArray]

  /**
    * Optional A* heuristic: lower means closer to the goal.
    * Default `0` keeps uninformed best-first behavior.
    * Must be admissible (never overestimate remaining cost) when using
    * [[com.barrybecker4.optimization.strategy.statespace.StateSpaceSearchMode.AStar]].
    */
  def heuristic(state: ParameterArray): Double = 0.0
}

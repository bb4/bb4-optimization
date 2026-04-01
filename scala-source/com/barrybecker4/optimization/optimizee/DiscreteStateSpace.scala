// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.ParameterArray

/**
  * Mix in with an [[Optimizee]] when the search space is a discrete graph and you can enumerate
  * one-step transitions. Used by [[com.barrybecker4.optimization.strategy.StateSpaceSearchStrategy]].
  * Successor lists should stay small; this API is for structured, low-branching problems—not huge graphs.
  */
trait DiscreteStateSpace {

  /** States reachable in one step from `state` (no duplicates required). */
  def successors(state: ParameterArray): Seq[ParameterArray]
}

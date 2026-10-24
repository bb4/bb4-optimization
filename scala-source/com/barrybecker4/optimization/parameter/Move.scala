// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

/**
  * A small change to a [[ParameterArray]], described instead of carried out. A strategy asks an array for a move
  * ([[ParameterArray.proposeMove]]), asks the optimizee what the fitness would be after it
  * ([[com.barrybecker4.optimization.optimizee.Optimizee.evaluateMove]]), and only builds the new array
  * ([[ParameterArray.applyMove]]) if it keeps the move. An optimizee that can work out the change in fitness from the
  * move alone (a swap touches a handful of edges of a tour, not all of them) never has to look at the whole array.
  *
  * Each kind of array defines its own moves and understands only those. Extend this trait for a new kind.
  */
trait Move

/** Exchange the values at positions `first` and `second` of a permutation. */
final case class Swap(first: Int, second: Int) extends Move

/** Several swaps, applied one after the other (a later swap sees the result of the earlier ones). */
final case class SwapSequence(swaps: IndexedSeq[Swap]) extends Move

/** Set the parameter at `index` to `value`, for an array of integers (or booleans, which are 0 and 1). */
final case class SetValue(index: Int, value: Double) extends Move

/**
  * A value at a position. It is what a move creates or destroys, as far as tabu search is concerned: after a swap,
  * two values are in new positions, and putting either back would undo it. See [[ParameterArray.createdAttributes]].
  */
final case class Placement(position: Int, value: Double)

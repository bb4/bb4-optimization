// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

/**
  * @param useMoves on a discrete problem, ask the optimizee for the fitness after a move
  *                 ([[com.barrybecker4.optimization.optimizee.Optimizee.evaluateMove]]) and build a neighbor only if
  *                 it improves, instead of building and evaluating every try. This is faster when the optimizee works
  *                 out the new fitness without a full evaluation, and the array supports moves. It has a price: the
  *                 cache of tries is not used, so a neighbor that was tried before may be evaluated again, which the
  *                 default avoids. Off by default.
  *
  *                 On the benchmark tours this did not help hill climbing and was slower on short ones: after a few
  *                 failed tries the radius is so large that a try is a shuffle, and no optimizee has a cheap delta
  *                 for that. Turn it on only if yours has a delta for large moves.
  */
final case class HillClimbingConfig(useMoves: Boolean = false)

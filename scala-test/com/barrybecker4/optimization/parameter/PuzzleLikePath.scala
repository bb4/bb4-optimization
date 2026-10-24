// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.Parameter

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random

/**
  * A permutation that picks its own neighbors and carries something more than its parameters, like a puzzle path
  * does. It is here to check that strategies keep using such an array's own `getRandomNeighbor`.
  */
class PuzzleLikePath(values: IndexedSeq[Parameter], val label: String, rnd: Random = new Random(1),
                     val asked: AtomicInteger = new AtomicInteger(0))
  extends PermutedParameterArray(values, rnd) {

  /** How many times a neighbor was asked of this array or any made from it. */
  def neighborsAsked: Int = asked.get

  override def getRandomNeighbor(radius: Double): PermutedParameterArray = {
    asked.incrementAndGet()
    new PuzzleLikePath(super.getRandomNeighbor(radius).params, label, rnd, asked)
  }

  override def forkWithRnd(newRnd: Random): PermutedParameterArray = new PuzzleLikePath(params, label, newRnd, asked)
}

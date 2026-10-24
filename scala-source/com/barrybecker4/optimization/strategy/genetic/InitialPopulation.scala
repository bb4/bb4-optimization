// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Proposes the candidates that, together with the initial guess, make up the first generation. */
private[genetic] object InitialPopulation {

  /** Tries this many times per wanted member before giving up on a small search space. */
  private val ATTEMPTS_PER_MEMBER = 100

  /**
    * Distinct random neighbors of the seed, enough to bring the population (seed included) up to `desiredSize`.
    * Fewer are returned when the space is too small to find that many distinct neighbors.
    * @param repair what the problem does to put right a neighbor that is not acceptable as it is; none by default
    */
  def proposeNeighbors(seed: ParameterArray, desiredSize: Int, radius: Double,
                       repair: ParameterArray => ParameterArray = identity): Seq[ParameterArray] = {
    val seen = mutable.HashSet(Solutions.key(seed))
    val neighbors = ArrayBuffer[ParameterArray]()
    val maxAttempts = ATTEMPTS_PER_MEMBER * desiredSize
    var attempts = 0
    while (1 + neighbors.size < desiredSize && attempts < maxAttempts) {
      val nbr = repair(seed.getRandomNeighbor(radius))
      if (seen.add(Solutions.key(nbr))) neighbors += nbr
      attempts += 1
    }
    neighbors.toSeq
  }
}

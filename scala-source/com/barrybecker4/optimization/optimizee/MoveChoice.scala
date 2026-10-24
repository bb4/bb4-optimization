// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{Move, ParameterArray}

import scala.util.Random

/**
  * Chooses between a random move and one the problem suggests. With a guided probability of 0 no random number is
  * drawn here, so a run that does not use guided moves sees the same random stream as before they existed.
  */
object MoveChoice {

  /**
    * @param guidedProbability the chance, in [0, 1], of asking the problem for a move
    * @return the problem's move when it was asked and had one, otherwise a random move of the given radius
    */
  def propose(optimizee: Optimizee, current: ParameterArray, radius: Double,
              guidedProbability: Double, rnd: Random): Move =
    if (shouldAskProblem(guidedProbability, rnd))
      optimizee.proposeGuidedMove(current, radius, rnd).getOrElse(current.proposeMove(radius))
    else current.proposeMove(radius)

  private def shouldAskProblem(guidedProbability: Double, rnd: Random): Boolean =
    guidedProbability > 0.0 && rnd.nextDouble() < guidedProbability
}

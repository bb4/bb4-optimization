// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter


/**
  * @param fitness fitness score associated with this parameter array.
  *                Lower values have better fitness.
  * @author Barry Becker
  */
case class ParameterArrayWithFitness(pa: ParameterArray, fitness: Double)

object ParameterArrayWithFitness {

  /** Natural ordering: lower fitness is better. */
  given Ordering[ParameterArrayWithFitness] = Ordering.by(_.fitness)
}

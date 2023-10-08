// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter


/**
  * @param fitness fitness score associated with this parameter array.
  *                Lower values have better fitness.
  * @author Barry Becker
  */
case class ParameterArrayWithFitness[P <: ParameterArray](pa: P, fitness: Double)
  extends Comparable[ParameterArrayWithFitness[P]] {


  /** Natural ordering based on the fitness evaluation assigned to this parameter array.
    * @param params the parameter array to compare ourselves too.
    * @return -1 if we are less than params, 1 if greater than params, 0 if equal.
    */
  override def compareTo(params: ParameterArrayWithFitness[P]): Int = {
    val diff = this.fitness - params.fitness
    if (diff < 0) return -1
    if (diff > 0) 1
    else 0
  }
}

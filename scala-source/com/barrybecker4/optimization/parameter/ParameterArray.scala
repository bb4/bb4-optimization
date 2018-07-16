// Copyright by Barry G. Becker, 2012 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.types.Parameter


/**
  * Represents an array of parameters.
  * Derived classes should be immutable.
  * @author Barry Becker
  */
trait ParameterArray {

  /** Some parameter arrays may have variable numbers of parameters.
    * @return the number of parameters in the array.
    */
  def size: Int

  /** @return the ith parameter in the array. Consider using apply instead. */
  def get(i: Int): Parameter

  /** @return a reasonable size for a sampling of values from the parameter space.*/
  def getSamplePopulationSize: Int

  /** Globally sample the parameter space with a uniform distribution.
    * @param requestedNumSamples approximate number of samples to retrieve.
    *   If the problem space is small and requestedNumSamples is large, it may not be possible to return this
    *   many unique samples. In this case all possibilities will be returned.
    * @return an iterator that is capable of producing the specified number of unique samples.
    */
  def findGlobalSamples(requestedNumSamples: Long): Iterator[_ <: ParameterArray]

  /** Some measure of the distance between parameter arrays of the same type.
    * The way this is computed is very implementation dependent.
    * @return a measure of the distance between this parameter array and another.
    */
  def distance(pa: ParameterArray): Double

  /** @param radius the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *               (relative to each parameter range).
    * @return the random neighbor.
    */
  def getRandomNeighbor(radius: Double): ParameterArray

  /** @return get a completely random solution in the parameter space.*/
  def getRandomSample: ParameterArray

  override def toString: String

  /** @return the parameters in a string of Comma Separated Values.*/
  def toCSVString: String
}
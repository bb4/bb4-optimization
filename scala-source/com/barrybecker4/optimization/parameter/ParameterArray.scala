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
  def findGlobalSamples(requestedNumSamples: Long): Iterator[? <: ParameterArray]

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

  /**
    * Whether [[proposeMove]] and [[applyMove]] work for this array. A strategy that finds out the fitness of a neighbor
    * from a move, rather than from the neighbor itself, only does so when this is true. False by default.
    */
  def supportsMoves: Boolean = false

  /**
    * A random move to a neighbor, drawing the same random numbers as [[getRandomNeighbor]] for the same radius, so that
    * `applyMove(proposeMove(r))` is the neighbor `getRandomNeighbor(r)` would have been.
    * @param radius the size of the move, as for [[getRandomNeighbor]]
    */
  def proposeMove(radius: Double): Move =
    throw new UnsupportedOperationException(getClass.getName + " does not support moves")

  /** @return the array after the move; this array is not changed. */
  def applyMove(move: Move): ParameterArray =
    throw new UnsupportedOperationException(getClass.getName + " does not support moves")

  /**
    * What makes two arrays of the same class the same candidate solution. Strategies that avoid evaluating or keeping
    * duplicates (such as genetic search) compare this, so equal identities must mean equal candidates.
    *
    * The default is a [[ValuesKey]] of the parameter values, which is right whenever the values fully describe the candidate.
    * Override it when they do not. For example, when the parameters are only an encoding that is relative to the
    * array itself (ids ranked within one particular path), different candidates can have the same values.
    * The result needs value equality and a consistent `hashCode` (a case class, a collection, a string, a [[ValuesKey]]).
    */
  def candidateIdentity: Any = ValuesKey.of(this)

  override def toString: String

  /** @return the parameters in a string of Comma Separated Values.*/
  def toCSVString: String
}
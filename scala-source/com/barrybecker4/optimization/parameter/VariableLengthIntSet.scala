// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter.distancecalculators.{DistanceCalculator, MagnitudeDistanceCalculator}
import com.barrybecker4.optimization.parameter.sampling.VariableLengthGlobalSampler
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.types.Parameter
import com.barrybecker4.optimization.parameter.VariableLengthIntSet._
import scala.util.Random


object VariableLengthIntSet {

  /** The larger this number is the less likely we are to add/remove ints when finding a random neighbor */
  private val ADD_REMOVE_RADIUS_SOFTENER = 0.5  // was 0.5

  private val DIST_CALCULATOR = new MagnitudeDistanceCalculator()

  def createInstance(params: IndexedSeq[Parameter], fullSeq: IndexedSeq[Int], rnd: Random): VariableLengthIntSet =
    new VariableLengthIntSet(params, fullSeq, DIST_CALCULATOR, rnd)

  /** @return one of the int params in the array with specified value */
  def createParam(i: Int): Parameter = {
    val min = if (i < 0) i else 0
    val max = if (i >= 0) i else 0
    new IntegerParameter(i, min, max, "p" + i)
  }
}

/**
  * Represents a 1 dimensional, variable length, set of unique integer parameters.
  * The order of the integers does not matter, but there cannot be duplicates. Immutable.
  * @param params  an array of params to initialize with.
  * @param fullSeq the full set of all integer parameters.
  * @author Barry Becker
  */
class VariableLengthIntSet(params: IndexedSeq[Parameter], val fullSeq: IndexedSeq[Int],
                           distCalc: DistanceCalculator = DIST_CALCULATOR, rnd: Random)
  extends AbstractParameterArray(params, rnd) {

  private val paramSet = params.toSet
  private val fullSet: Set[Int] = fullSeq.toSet

  /** @return the maximum length of the variable length array */
  def getMaxLength: Int = fullSet.size

  def intValues: IndexedSeq[Int] = params.map(_.getValue.toInt)

  /** The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two instances.
    * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
    *  - the length of the parameter array
    *  - the set of values in the parameter array.
    * Generally, the distance is greater the greater the number of parameters that are different.
    * @return the distance between this parameter array and another.
    */
  override def distance(pa: ParameterArray): Double = distCalc.calculateDistance(this, pa)

  /** Create a new permutation that is not too distant from what we have now.
    * The two ways a configuration of marked nodes can change is
    *  - add or remove nodes
    *  - change values of nodes
    * @param radius an indication of the amount of variation to use. 0 is none, 2 is a lot.
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): VariableLengthIntSet = {
    if (size < 1) return this
    val probAddRemove = radius / (ADD_REMOVE_RADIUS_SOFTENER + radius)
    var add = false
    var remove = false
    if (rnd.nextDouble() < probAddRemove) {
      if ((rnd.nextDouble() > 0.5 || size <= 1) && size < getMaxLength - 1) add = true
      else remove = true
    }
    var numNodesToMove =
      if (add || remove) rnd.nextInt(Math.min(size, (radius + 1.1).toInt))
      else 1 + rnd.nextInt((1.4 + radius).toInt) // at least 1 will be moved

    var result =
      if (remove) removeRandomParams(numToRemove(radius))
      else if (add) addRandomParams(numToAdd(radius))
      else this
    assert(add || remove || numNodesToMove > 0)
    result.moveNodes(numNodesToMove)
  }

  private def numToRemove(radius: Double): Int =
    skewedNumToSelect(radius, params.length - 2)
  private def numToAdd(radius: Double): Int =
    skewedNumToSelect(radius, fullSet.size - params.size)

  private def skewedNumToSelect(radius: Double, len: Int): Int = {
    val upper = (radius / 2.0 * Math.abs(rnd.nextGaussian()) * len).toInt
    Math.max(1, Math.min(len, upper))
  }

  /** @return an instance with specified indices from fullSet */
  def getCombination(indices: Seq[Int]): VariableLengthIntSet = {
    assert(indices.size <= getMaxLength,
      "The number of indices (" + indices.size + ") was greater than the max size (" + size + ")")
    val newParams = for (i <- indices) yield createParam(fullSeq(i))
    new VariableLengthIntSet(newParams.toIndexedSeq, fullSeq, distCalc, rnd)
  }

  /** Globally sample the parameter space.
    * @param requestedNumSamples approximate number of samples to retrieve.
    *     If the problem space is small and requestedNumSamples is large, it may not be possible to return this
    *     many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long): Iterator[VariableLengthIntSet] =
    new VariableLengthGlobalSampler(this, requestedNumSamples)

  /** @return get a random solution in the parameter space by selecting about half of the ints */
  override def getRandomSample: ParameterArray = {
    val shuffled = rnd.shuffle(fullSeq)
    val marked = shuffled.take(((shuffled.length - 1) * rnd.nextDouble()).toInt + 1)
    val newParams = marked.map(m => createParam(m))
    new VariableLengthIntSet(newParams, fullSeq, distCalc, rnd)
  }

  /** @param i the integer parameter's value. May be Negative
    * @return a new integer parameter.
    */
  private def createParam(i: Int) =
    new IntegerParameter(i,
      if (i < 0) i else 0,
      if (i >= 0) i else 0,
      "p" + i)

  private def removeRandomParams(num: Int): VariableLengthIntSet = {
    val rndIndices = rnd.shuffle(params.indices.toIndexedSeq)
    val indicesToRemove = rndIndices.take(num).toSet
    //println("removing " + indicesToRemove.mkString(", ") + " from " + params.length)
    new VariableLengthIntSet(params.zipWithIndex.filter(p => !indicesToRemove.contains(p._2)).map(_._1),
      fullSeq, distCalc, rnd)
  }

  private def addRandomParams(num: Int): VariableLengthIntSet = {
    val intsToAdd = rnd.shuffle(getFreeNodes).take(num)
    assert(size + intsToAdd.size <= getMaxLength)
    new VariableLengthIntSet(params ++ intsToAdd.map(createParam), fullSeq, distCalc, rnd)
  }

  /** Select num free nodes randomly and swap them with num randomly selected marked nodes.
    * If there are no free nodes, then we must resort to removing one so something changes.
    * @param numNodesToMove number of nodes to move to new locations
    */
  private def moveNodes(numNodesToMove: Int): VariableLengthIntSet = {
    if (numNodesToMove == 0) return this
    val freeNodes = getFreeNodes
    val numSelect = Array(freeNodes.size, params.size, numNodesToMove).min
    if (numSelect == 0) {
      removeRandomParams(1)
    } else {

      val randomIndices = rnd.shuffle(params.indices.toIndexedSeq).take(numSelect).sorted
      val randomFreeIndices = rnd.shuffle(freeNodes.indices.toIndexedSeq).take(numSelect)

      var ct = 0
      //println("current set = " + params.map(_.getValue).mkString(", "))
      //println("rnd indices to swap: " + randomIndices.mkString(", "))
      //println("free nodes: " + freeNodes.mkString(" "))

      val newParams = for (i <- params.indices) yield {   // debug
        if (ct < numSelect && i == randomIndices(ct)) {
          val v = freeNodes(randomFreeIndices(ct))
          ct += 1
          createParam(v)
        } else params(i)
      }
      //println("new paramValss after exchanges: " + newParams.map(_.getValue).mkString(", "))
      new VariableLengthIntSet(newParams, fullSeq, distCalc, rnd)
    }
  }

  /** @return all the ints in fullSet that are not currently used */
  private def getFreeNodes: Seq[Int] = {
    val markedNodes = (
      for (p <- params)
        yield p.getValue.toInt
      ).toSet
    fullSet.diff(markedNodes).toSeq
  }


  def canEqual(other: Any): Boolean = other.isInstanceOf[VariableLengthIntSet]

  /** @return true if equal. The values must be the same, but the order does not matter */
  override def equals(other: Any): Boolean = other match {
    case that: VariableLengthIntSet =>
      (that canEqual this) &&
        paramSet == that.paramSet &&
        fullSet == that.fullSet
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(paramSet, fullSet)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
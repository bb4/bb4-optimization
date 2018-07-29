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
  private val ADD_REMOVE_RADIUS_SOFTENER = 0.5

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
    if (rnd.nextDouble < probAddRemove) {
      if ((rnd.nextDouble > 0.5 || size <= 1) && size < getMaxLength - 1) add = true
      else remove = true
    }
    var numNodesToMove = 0
    //println(s"rad=$radius pAdd/Rm=$probAddRemove add=$add remove=$remove")
    if (add || remove) numNodesToMove = rnd.nextInt(Math.min(size, (radius + 1.4).toInt))
    else { // at least 1 will be moved
      numNodesToMove = 1 + rnd.nextInt((1.1 + radius).toInt)
    }
    if (remove) {
      // possibly add more that one depending on radius
      removeRandomParam()
    }
    if (add) {
      // possibly remove more that one depending on radius
      addRandomParam()
    }
    moveNodes(numNodesToMove)
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
  override def findGlobalSamples(requestedNumSamples: Long) =
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

  private def removeRandomParam(): VariableLengthIntSet = {
    val indexToRemove = rnd.nextInt(size)
    new VariableLengthIntSet(params.zipWithIndex.filter(p => p._2 != indexToRemove).map(_._1),
      fullSeq, distCalc, rnd)
  }

  private def addRandomParam(): VariableLengthIntSet = {
    val freeNodes = getFreeNodes
    val newSize = size + 1
    assert(newSize <= getMaxLength)
    var newParams = for (p <- params) yield p

    // randomly add one one of the free nodes to the list
    val value: Int = freeNodes(rnd.nextInt(freeNodes.size))
    newParams :+= createParam(value)
    new VariableLengthIntSet(newParams, fullSeq, distCalc, rnd)
  }

  /** Select num free nodes randomly and swap them with num randomly selected marked nodes.
    * If there are no free nodes, then we must resort to removing one so something changes.
    * @param numNodesToMove number of nodes to move to new locations
    */
  private def moveNodes(numNodesToMove: Int): VariableLengthIntSet = {
    val freeNodes = getFreeNodes
    val numSelect = Math.min(freeNodes.size, numNodesToMove)
    if (numSelect == 0) {
      removeRandomParam()
    } else {
      val swapNodes = selectRandomNodes(numSelect, freeNodes)
      val newParams = params.toArray

      val randomIndices = (for (i <- 0 until numSelect) yield rnd.nextInt(freeNodes.size)).sorted
      var ct = 0

      for (i <- params.indices) yield {
        if (ct < numSelect && i == randomIndices(ct)) {
          val v = freeNodes(randomIndices(ct))
          ct += 1
          createParam(v)
        } else params(i)
      }
      new VariableLengthIntSet(newParams, fullSeq, distCalc, rnd)
    }
  }

  private def selectRandomNodes(numNodesToSelect: Int, freeNodes: Seq[Int]): Seq[Int] = {
    rnd.shuffle(freeNodes).take(numNodesToSelect)
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
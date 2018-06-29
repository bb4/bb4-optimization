// Copyright by Barry G. Becker, 2013 - 2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.distancecalculators.DistanceCalculator
import com.barrybecker4.optimization.parameter.distancecalculators.MagnitudeIgnoredDistanceCalculator
import com.barrybecker4.optimization.parameter.improvement.DiscreteImprovementFinder
import com.barrybecker4.optimization.parameter.improvement.Improvement
import com.barrybecker4.optimization.parameter.sampling.VariableLengthGlobalSampler
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.parameter.types.Parameter

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


object VariableLengthIntArray {

  /** The larger this number is the less likely we are to add/remove ints when finding a random neighbor */
  private val ADD_REMOVE_RADIUS_SOFTENER = 0.6

  def createInstance(params: Array[Parameter], fullSet: Set[Int]): VariableLengthIntArray =
    createInstance(params, fullSet, new MagnitudeIgnoredDistanceCalculator())

  def createInstance(params: Array[Parameter], fullSet: Set[Int], distanceCalculator: DistanceCalculator) =
    new VariableLengthIntArray(params, fullSet, distanceCalculator)
}

/**
  * Represents a 1 dimensional, variable length, array of unique integer parameters.
  * The order of the integers does not matter, but there cannot be duplicates.
  * @author Barry Becker
  */
class VariableLengthIntArray(theParams: Array[Parameter]) extends AbstractParameterArray(theParams) {

  private var fullSet: Set[Int] = _
  private var fullSeq: Seq[Int] = _
  private var distCalculator: DistanceCalculator = _

  /**
    * Constructor
    * @param params  an array of params to initialize with.
    * @param fullSet the full set of all integer parameters.
    */
  def this(params: Array[Parameter], fullSet: Set[Int], distCalc: DistanceCalculator) {
    this(params)
    this.fullSet = fullSet
    this.fullSeq = fullSet.toArray
    assert(distCalc != null)
    this.distCalculator = distCalc
  }

  /** @return the maximum length of the variable length array */
  def getMaxLength: Int = fullSet.size

  override protected def createInstance = new VariableLengthIntArray(Array[Parameter]())

  /** The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two instances.
    * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
    *  - the length of the parameter array
    *  - the set of values in the parameter array.
    * Generally, the distance is greater the greater the number of parameters that are different.
    * @return the distance between this parameter array and another.
    */
  override def distance(pa: ParameterArray): Double = distCalculator.calculateDistance(this, pa)

  /** Create a new permutation that is not too distant from what we have now.
    * The two ways a configuration of marked nodes can change is
    *  - add or remove nodes
    *  - change values of nodes
    * @param radius an indication of the amount of variation to use. 0 is none, 2 is a lot.
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): VariableLengthIntArray = {
    if (size < 1) return this
    val probAddRemove = radius / (VariableLengthIntArray.ADD_REMOVE_RADIUS_SOFTENER + radius)
    var add = false
    var remove = false
    if (MathUtil.RANDOM.nextDouble < probAddRemove) {
      if ((MathUtil.RANDOM.nextDouble > 0.5 || size <= 1) && size < getMaxLength - 1) add = true
      else remove = true
    }
    var numNodesToMove = 0
    val nbr = this.copy.asInstanceOf[VariableLengthIntArray]
    println(s"rad=$radius pAdd/Rm=$probAddRemove add=$add remove=$remove")
    if (add || remove) numNodesToMove = MathUtil.RANDOM.nextInt(Math.min(size, (radius + 1.5).toInt))
    else { // at least 1 will be moved
      numNodesToMove = 1 + MathUtil.RANDOM.nextInt((1.5 + radius).toInt)
    }
    if (remove) removeRandomParam(nbr)
    if (add) addRandomParam(nbr)
    moveNodes(numNodesToMove, nbr)
    nbr
  }

  /** Add method to get a neighbor that improves some specified cost function */

  def getCombination(indices: Seq[Int]): VariableLengthIntArray = {
    assert(indices.size <= getMaxLength,
      "The number of indices (" + indices.size + ") was greater than the size (" + size + ")")
    var newParams = for (i <- indices) yield createParam(fullSeq(i))
    new VariableLengthIntArray(newParams.toArray, fullSet, distCalculator)
  }

  /** Globally sample the parameter space.
    * @param requestedNumSamples approximate number of samples to retrieve.
    *     If the problem space is small and requestedNumSamples is large, it may not be possible to return this
    *     many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long) =
    new VariableLengthGlobalSampler(this, requestedNumSamples)

  /** Try swapping parameters randomly until we find an improvement (if we can). */
  override def findIncrementalImprovement(optimizee: Optimizee, jumpSize: Double,
                                          lastImprovement: Improvement,
                                          cache: mutable.Set[ParameterArray]): Improvement = {
    val finder = new DiscreteImprovementFinder(this)
    finder.findIncrementalImprovement(optimizee, jumpSize, cache)
  }

  /** @return get a random solution in the parameter space by selecting about half of the ints */
  override def getRandomSample: ParameterArray = {
    val shuffled = MathUtil.RANDOM.shuffle(fullSeq.toSeq)
    var marked = shuffled.take((shuffled.length + 1) / 2)
    var newParams = marked.map(m => createParam(m))
    new VariableLengthIntArray(newParams.toArray, fullSet, distCalculator)
  }

  /** @return a copy of ourselves */
  override def copy: AbstractParameterArray = {
    val copy = super.copy.asInstanceOf[VariableLengthIntArray]
    copy.fullSet = this.fullSet
    copy.distCalculator = this.distCalculator
    copy
  }

  /** @param i the integer parameter's value. May be Negative
    * @return a new integer parameter.
    */
  private def createParam(i: Int) = new IntegerParameter(i, if (i < 0) i
    else 0, if (i >= 0) i
    else 0, "p" + i)

  private def removeRandomParam(nbr: VariableLengthIntArray): Unit = {
    val indexToRemove = MathUtil.RANDOM.nextInt(size)
    assert(nbr.size > 0)
    nbr.params = nbr.params.zipWithIndex.filter(p => p._2 != indexToRemove).map(_._1)
  }

  private def addRandomParam(nbr: VariableLengthIntArray): Unit = {
    val freeNodes = getFreeNodes(nbr)
    val newSize = nbr.size + 1
    assert(newSize <= getMaxLength)
    var newParams = for (p <- nbr.params) yield p

    // randomly add one one of the free nodes to the list
    val value: Int = freeNodes(MathUtil.RANDOM.nextInt(freeNodes.size))
    newParams :+= createParam(value)
    nbr.params = newParams
  }

  /** Select num free nodes randomly and swap them with num randomly selected marked nodes.
    * @param numNodesToMove number of nodes to move to new locations
    * @param nbr            neighbor parameter array
    */
  private def moveNodes(numNodesToMove: Int, nbr: VariableLengthIntArray): Unit = {
    val freeNodes = getFreeNodes(nbr)
    val numSelect = Math.min(freeNodes.size, numNodesToMove)
    val swapNodes = selectRandomNodes(numSelect, freeNodes)
    for (i <- 0 until numSelect) {
      val index = MathUtil.RANDOM.nextInt(nbr.size)
      nbr.get(index).setValue(swapNodes(i))
    }
  }

  private def selectRandomNodes(numNodesToSelect: Int, freeNodes: Seq[Int]): Seq[Int] = {
    MathUtil.RANDOM.shuffle(freeNodes).take(numNodesToSelect)
  }

  /** @return all the ints in fullSet that are not in nbr currently */
  private def getFreeNodes(nbr: VariableLengthIntArray): Seq[Int] = {
    val markedNodes = (
      for (p <- nbr.params)
        yield p.getValue.toInt
      ).toSet
    fullSet.diff(markedNodes).toSeq
  }
}

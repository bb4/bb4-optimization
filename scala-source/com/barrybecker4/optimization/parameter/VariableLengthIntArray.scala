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

  def createInstance(params: Array[Parameter], fullSet: Array[Int]): VariableLengthIntArray =
    createInstance(params, fullSet, new MagnitudeIgnoredDistanceCalculator())

  def createInstance(params: Array[Parameter], fullSet: Array[Int], distanceCalculator: DistanceCalculator) =
    new VariableLengthIntArray(params, fullSet, distanceCalculator)
}

/**
  * Represents a 1 dimensional, variable length, array of unique integer parameters.
  * The order of the integers does not matter.
  * @author Barry Becker
  */
class VariableLengthIntArray(theParams: Array[Parameter]) extends AbstractParameterArray(theParams) {

  private var fullSet: Array[Int] = _
  private var distCalculator: DistanceCalculator = _

  /**
    * Constructor
    * @param params  an array of params to initialize with.
    * @param fullSet the full set of all integer parameters.
    */
  def this(params: Array[Parameter], fullSet: Array[Int], distCalc: DistanceCalculator) {
    this(params)
    this.fullSet = fullSet
    assert(distCalc != null)
    this.distCalculator = distCalc
  }

  /** @return the maximum length of the variable length array */
  def getMaxLength: Int = fullSet.length

  override protected def createInstance = new VariableLengthIntArray(Array[Parameter]())

  /**
    * The distance computation will be quite different for this than a regular parameter array.
    * We want the distance to represent a measure of the amount of similarity between two instances.
    * There are two ways in which instance can differ, and the weighting assigned to each may depend on the problem.
    *  - the length of the parameter array
    *  - the set of values in the parameter array.
    * Generally, the distance is greater the greater the number of parameters that are different.
    *
    * @return the distance between this parameter array and another.
    */
  override def distance(pa: ParameterArray): Double = distCalculator.calculateDistance(this, pa)

  /**
    * Create a new permutation that is not too distant from what we have now.
    * The two ways a configuration of marked nodes can change is
    *  - add or remove nodes
    *  - change values of nodes
    *
    * @param radius an indication of the amount of variation to use. 0 is none, 2 is a lot.
    *               Change Math.min(1, 10 * radius * N/100) of the entries, where N is the number of params
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): VariableLengthIntArray = {
    if (size < 1) return this
    val probAddRemove = radius / (VariableLengthIntArray.ADD_REMOVE_RADIUS_SOFTENER + radius)
    var add = false
    var remove = false
    if (MathUtil.RANDOM.nextDouble > probAddRemove) if ((MathUtil.RANDOM.nextDouble > 0.5 || size <= 1) && size < getMaxLength - 1) add = true
    else remove = true
    var numNodesToMove = 0
    val nbr = this.copy.asInstanceOf[VariableLengthIntArray]
    if (add || remove) numNodesToMove = MathUtil.RANDOM.nextInt(Math.min(size, (radius + 1.5).toInt))
    else { // at least 1 will be moved
      numNodesToMove = 1 + MathUtil.RANDOM.nextInt((1.5 + radius).toInt)
    }
    if (remove) removeRandomParam(nbr)
    if (add) addRandomParam(nbr)
    moveNodes(numNodesToMove, nbr)
    nbr
  }

  def setCombination(indices: Seq[Int]): Unit = {
    assert(indices.size <= getMaxLength, "The number of indices (" + indices.size + ") was greater than the size (" + size + ")")
    var newParams = Array.ofDim[Parameter](size)

    for (i <- indices) {
      newParams :+= createParam(fullSet(i))
    }
    params = newParams
  }

  /** Globally sample the parameter space.
    * @param requestedNumSamples approximate number of samples to retrieve.
    *     If the problem space is small and requestedNumSamples is large, it may not be possible to return this
    *     many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long) = new VariableLengthGlobalSampler(this, requestedNumSamples)

  /** Try swapping parameters randomly until we find an improvement (if we can). */
  override def findIncrementalImprovement(optimizee: Optimizee, jumpSize: Double,
                        lastImprovement: Improvement, cache: mutable.Set[ParameterArray]): Improvement = {
    val finder = new DiscreteImprovementFinder(this)
    finder.findIncrementalImprovement(optimizee, jumpSize, cache)
  }

  /**
    * @return get a completely random solution in the parameter space.
    */
  override def getRandomSample: ParameterArray = {
    var marked = List[Int]()
    for (i <- 0 until getMaxLength) {
      if (MathUtil.RANDOM.nextDouble > 0.5)
        marked +:= fullSet(i)
    }
    var newParams = Array.ofDim[Parameter](marked.length)

    for (markedNode <- marked) {
      newParams :+= createParam(markedNode)
    }
    new VariableLengthIntArray(newParams, fullSet, distCalculator)
  }

  /**
    * @return a copy of ourselves.
    */
  override def copy: AbstractParameterArray = {
    val copy = super.copy.asInstanceOf[VariableLengthIntArray]
    copy.fullSet = this.fullSet
    copy.distCalculator = this.distCalculator
    copy
  }

  /**
    * @param i the integer parameter's value. May be Negative
    * @return a new integer parameter.
    */
  private def createParam(i: Int) = new IntegerParameter(i, if (i < 0) i
    else 0, if (i >= 0) i
    else 0, "p" + i)

  private def removeRandomParam(nbr: VariableLengthIntArray): Unit = {
    val indexToRemove = MathUtil.RANDOM.nextInt(size)
    assert(nbr.size > 0)
    var newParams = Array.ofDim[Parameter](nbr.size - 1)
    for (i <- 0 until nbr.size) {
      if (i != indexToRemove)
        newParams :+= nbr.get(i)
    }
    nbr.params = newParams
  }

  private def addRandomParam(nbr: VariableLengthIntArray): Unit = {
    val freeNodes = getFreeNodes(nbr)
    val newSize = nbr.size + 1
    assert(newSize <= getMaxLength)
    var newParams = Array.ofDim[Parameter](newSize)
    for (p <- nbr.params) {
      newParams :+= p
    }

    // randomly add one one of the free nodes to the list
    val value = freeNodes(MathUtil.RANDOM.nextInt(freeNodes.size))
    newParams :+= createParam(value)
    nbr.params = newParams
  }

  /** Select num free nodes randomly and and swap them with num randomly selected marked nodes.
    * @param numNodesToMove number of nodes to move to new locations
    * @param nbr            neighbor parameter array
    */
  private def moveNodes(numNodesToMove: Int, nbr: VariableLengthIntArray): Unit = {
    val freeNodes = getFreeNodes(nbr)
    val numSelect = Math.min(freeNodes.length, numNodesToMove)
    val swapNodes = selectRandomNodes(numSelect, freeNodes)
    for (i <- 0 until numSelect) {
      val index = MathUtil.RANDOM.nextInt(nbr.size)
      nbr.get(index).setValue(swapNodes(i))
    }
  }

  private def selectRandomNodes(numNodesToSelect: Int, freeNodes: ArrayBuffer[Int]): Array[Int] = {
    var selected = Array.ofDim[Int](numNodesToSelect)
    for (i <- 0 until numNodesToSelect) {
      val node = freeNodes(MathUtil.RANDOM.nextInt(freeNodes.length))
      selected  :+= node
      freeNodes.remove(node.asInstanceOf[Integer])
    }
    selected
  }

  private def getFreeNodes(nbr: VariableLengthIntArray): ArrayBuffer[Int] = {
    var freeNodes = new ArrayBuffer[Int](getMaxLength)
    var markedNodes = Set[Int]()

    for (p <- nbr.params) {
      markedNodes += p.getValue.toInt
    }
    for (i <- 0 until getMaxLength) {
      if (!markedNodes.contains(fullSet(i)))
        freeNodes :+= fullSet(i)
    }
    freeNodes
  }
}

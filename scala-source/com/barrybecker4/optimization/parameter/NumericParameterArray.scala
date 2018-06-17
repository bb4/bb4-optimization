// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.improvement.Improvement
import com.barrybecker4.optimization.parameter.improvement.ImprovementIteration
import com.barrybecker4.optimization.parameter.improvement.ImprovementStep
import com.barrybecker4.optimization.parameter.sampling.NumericGlobalSampler
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, Parameter}
import scala.collection.mutable
import NumericParameterArray._


object NumericParameterArray {
  /** default number of steps to go from the min to the max */
  private val DEFAULT_NUM_STEPS = 10

  /** If the dot product of the new gradient with the old is less than this, then decrease the jump size. */
  private val MIN_DOT_PRODUCT = 0.3

  /** If the dot product of the new gradient with the old is greater than this, then increase the jump size. */
  private val MAX_DOT_PRODUCT = 0.98

  def createParams(vals: Array[Double], minVals: Array[Double], maxVals: Array[Double],
                   names: Array[String]): Array[Parameter] = {
    val len = vals.length
    var params = for (i <- 0 until len) yield {
       new DoubleParameter(vals(i), minVals(i), maxVals(i), names(i))
    }
    params.toArray
  }
}

/**
  * Represents a 1 dimensional array of parameters
  * @param theParams an array of params to initialize with.
  * @author Barry Becker
  */
class NumericParameterArray(theParams: Array[Parameter]) extends AbstractParameterArray(theParams) {

  private var numSteps = NumericParameterArray.DEFAULT_NUM_STEPS

  /** Constructor if all the parameters are DoubleParameters
    * @param vals the values for each parameter.
    * @param minVals the minimum value allowed for each parameter respectively.
    * @param maxVals the maximum value allowed for each parameter respectively.
    * @param names   the display name for each parameter in the array.
    */
  def this(vals: Array[Double], minVals: Array[Double], maxVals: Array[Double], names: Array[String]) {
    this(createParams(vals, minVals, maxVals, names))
  }

  /** @return a copy of ourselves.*/
  override def copy: NumericParameterArray = {
    val pa = super.copy.asInstanceOf[NumericParameterArray]
    pa.setNumSteps(numSteps)
    pa
  }

  override protected def createInstance = new NumericParameterArray(Array[Parameter]())

  /** Globally sample the parameter space with a uniform distribution.
    * @param requestedNumSamples approximate number of samples to retrieve. If the problem space is small
    *    and requestedNumSamples is large, it may not be possible to return this many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long): Iterator[NumericParameterArray] =
    new NumericGlobalSampler(this, requestedNumSamples)


  override def findIncrementalImprovement(optimizee: Optimizee, jumpSize: Double,
                      lastImprovement: Improvement, cache: mutable.Set[ParameterArray]): Improvement = {
    var currentParams = this
    var oldFitness = currentParams.getFitness
    var oldGradient: Vector = null
    if (lastImprovement != null) {
      oldFitness = lastImprovement.parameters.getFitness
      oldGradient = lastImprovement.gradient
    }
    val iter = new ImprovementIteration(this, oldGradient)
    var sumOfSqs: Double = 0
    for (i <- 0 until size) {
      val testParams = this.copy
      sumOfSqs = iter.incSumOfSqs(i, sumOfSqs, optimizee, currentParams, testParams)
    }
    val gradLength = Math.sqrt(sumOfSqs)
    val step = new ImprovementStep(optimizee, iter, gradLength, cache, jumpSize, oldFitness)
    currentParams = step.findNextParams(currentParams)
    var newJumpSize = step.getJumpSize
    // the improvement may be zero or negative, meaning it did not improve.
    val improvement = step.getImprovement
    val dotProduct = iter.gradient.normalizedDot(iter.oldGradient)
    //println("dot between " + iter.getGradient() + " and " + iter.getOldGradient()+ " is "+ dotProduct);
    newJumpSize = findNewJumpSize(newJumpSize, dotProduct)
    iter.gradient.copyFrom(iter.oldGradient)
    Improvement(currentParams, improvement, newJumpSize, iter.gradient)
  }

  /** If we are headed in pretty much the same direction as last time, then we increase the jumpSize.
    * If we are headed off in a completely new direction, reduce the jumpSize until we start to stabilize.
    * @param jumpSize   the current amount that is stepped in the assumed solution direction.
    * @param dotProduct determines the angle between the new gradient and the old.
    * @return the new jump size - which is usually the same as the old one.
    */
  private def findNewJumpSize(jumpSize: Double, dotProduct: Double) = {
    var newJumpSize = jumpSize
    if (dotProduct > NumericParameterArray.MAX_DOT_PRODUCT) newJumpSize *= ImprovementStep.JUMP_SIZE_INC_FACTOR
    else if (dotProduct < NumericParameterArray.MIN_DOT_PRODUCT) newJumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
    //println( "dotProduct = " + dotProduct + " new jumpsize = " + jumpSize );
    newJumpSize
  }

  /** @return the distance between this parameter array and another. sqrt(sum of squares) */
  override def distance(pa: ParameterArray): Double = {
    assert(size == pa.size)
    var sumOfSq = 0.0
    for (k <- 0 until size) {
      val dif = pa.get(k).getValue - get(k).getValue
      sumOfSq += dif * dif
    }
    Math.sqrt(sumOfSq)
  }

  /** Add a vector of deltas to the parameters.
    * @param vec must be the same size as the parameter list.
    */
  def add(vec: Vector): Unit = {
    assert(vec.size == size, "Parameter vec has magnitude " + vec.size + ", expecting " + size)
    for (i <- 0 until size) {
      val param = get(i)
      param.setValue(param.getValue + vec.get(i))
      if (param.getValue > param.maxValue) {
        println("Warning param " +
          param.name + " is exceeding is maximum value. It is being pegged to that maximum of " + param.maxValue)
        param.setValue(param.maxValue)
      }
      if (param.getValue < param.minValue) {
        println("Warning param " +
          param.name + " is exceeding is minimum value. It is being pegged to that minimum of " + param.minValue)
        param.setValue(param.minValue)
      }
    }
  }

  /** @param radius the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *               (relative to each parameter range).
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): NumericParameterArray = {
    val nbr = this.copy
    for (k <- 0 until size) {
      val param = nbr.get(k)
      param.tweakValue(radius, MathUtil.RANDOM)
    }
    nbr
  }

  /** @return get a completely random solution in the parameter space.*/
  override def getRandomSample: NumericParameterArray = {
    val nbr = this.copy
    for (k <- 0 until size) {
      val newPar = nbr.get(k)
      newPar.setValue(newPar.minValue + MathUtil.RANDOM.nextDouble * newPar.range)
      assert(newPar.getValue < newPar.maxValue && newPar.getValue > newPar.minValue, "newPar "
        + newPar.getValue + " not between " + newPar.minValue + " and  " + newPar.maxValue)
    }
    nbr
  }

  /** @return a new double array the same magnitude as the parameter list*/
  def asVector: Vector = {
    val v = new Vector(this.size)
    for (i <- 0 until this.size)
      v.set(i, this.get(i).getValue)
    v
  }

  def setNumSteps(numSteps: Int): Unit = {
    this.numSteps = numSteps
  }

  def getNumSteps: Int = numSteps
}
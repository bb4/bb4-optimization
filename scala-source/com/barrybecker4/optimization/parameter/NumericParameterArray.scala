// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.common.math.Vector
import com.barrybecker4.optimization.parameter.sampling.NumericGlobalSampler
import com.barrybecker4.optimization.parameter.types.{DoubleParameter, Parameter}
import NumericParameterArray._
import scala.util.Random


object NumericParameterArray {
  /** default number of steps to go from the min to the max */
  val DEFAULT_NUM_STEPS = 10

  def createParams(vals: IndexedSeq[Double], minVals: IndexedSeq[Double], maxVals: IndexedSeq[Double],
                   names: IndexedSeq[String]): IndexedSeq[Parameter] = {
    val len = vals.length
    val params = for (i <- 0 until len) yield {
      new DoubleParameter(vals(i), minVals(i), maxVals(i), names(i))
    }
    params
  }
}

/**
  * Represents a 1 dimensional array of parameters
  * @param params an array of params to initialize with.
  * @author Barry Becker
  */
case class NumericParameterArray(params: IndexedSeq[Parameter],
                                 numSteps: Int = DEFAULT_NUM_STEPS,
                                 rnd: Random = MathUtil.RANDOM)
  extends AbstractParameterArray(params, rnd) {

  /** Constructor if all the parameters are DoubleParameters
    * @param vals the values for each parameter.
    * @param minVals the minimum value allowed for each parameter respectively.
    * @param maxVals the maximum value allowed for each parameter respectively.
    * @param names   the display name for each parameter in the array.
    */
  def this(vals: IndexedSeq[Double], minVals: IndexedSeq[Double], maxVals: IndexedSeq[Double],
           names: IndexedSeq[String], rnd: Random) {
    this(createParams(vals, minVals, maxVals, names), DEFAULT_NUM_STEPS, rnd)
  }

  /** Constructor if all the parameters are DoubleParameters
    * @param vals the values for each parameter.
    * @param minVals the minimum value allowed for each parameter respectively.
    * @param maxVals the maximum value allowed for each parameter respectively.
    * @param names   the display name for each parameter in the array.
    */
  def this(vals: Array[Double], minVals: Array[Double], maxVals: Array[Double],
    names: Array[String], rnd: Random) {
    this(createParams(vals.toIndexedSeq, minVals.toIndexedSeq, maxVals.toIndexedSeq, names.toIndexedSeq),
      DEFAULT_NUM_STEPS, rnd)
  }

  /** Globally sample the parameter space with a uniform distribution.
    * @param requestedNumSamples approximate number of samples to retrieve. If the problem space is small
    *    and requestedNumSamples is large, it may not be possible to return this many unique samples.
    * @return some number of unique samples.
    */
  override def findGlobalSamples(requestedNumSamples: Long): Iterator[NumericParameterArray] =
    new NumericGlobalSampler(this, requestedNumSamples)

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
  def add(vec: Vector): NumericParameterArray = {
    assert(vec.size == size, "Parameter vec has magnitude " + vec.size + ", expecting " + size)

    val newParams = for (i <- 0 until size) yield {
      val param = get(i)
      var newParam = param.setValue(param.getValue + vec(i))
      if (newParam.getValue > newParam.maxValue) {
        println("Warning param " +
          newParam.name + " is exceeding is maximum value. It is being pegged to that maximum of " + newParam.maxValue)
        newParam = newParam.setValue(newParam.maxValue)
      }
      if (newParam.getValue < newParam.minValue) {
        println("Warning param " +
          newParam.name + " is exceeding is minimum value. It is being pegged to that minimum of " + newParam.minValue)
        newParam.setValue(newParam.minValue)
      }
      newParam
    }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  def incrementByEps(idx: Int, dir: Direction): NumericParameterArray = {
    val newParams = for (i <- 0 until size) yield {
      if (idx == i) get(i).incrementByEps(dir) else get(i)
    }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  /** @param radius the size of the (1 std deviation) gaussian neighborhood to select a random nbr from
    *               (relative to each parameter range).
    * @return the random nbr.
    */
  override def getRandomNeighbor(radius: Double): NumericParameterArray =
    NumericParameterArray(this.params.map(p => p.tweakValue(radius, rnd)), numSteps, rnd)

  /** @return get a completely random solution in the parameter space.*/
  override def getRandomSample: NumericParameterArray = {
    val newParams =
      for (k <- 0 until size) yield {
        val par = get(k)
        val newPar = par.setValue(par.minValue + rnd.nextDouble * par.range)
        assert(newPar.getValue < newPar.maxValue && newPar.getValue > newPar.minValue, "newPar "
          + newPar.getValue + " not between " + newPar.minValue + " and  " + newPar.maxValue)
        newPar
      }
    NumericParameterArray(newParams, numSteps, rnd)
  }

  /** @return a new double array the same magnitude as the parameter list*/
  def asVector: Vector = Vector(params.map(_.getValue))
}

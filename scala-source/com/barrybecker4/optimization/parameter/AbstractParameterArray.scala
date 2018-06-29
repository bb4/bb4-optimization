// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.types.Parameter


object AbstractParameterArray {
  /** Never exceed this amount  */
  private val POPULATION_MAX = 4000
}

/**
  * Represents a 1 dimensional array of parameters. This is the thing to be optimized.
  * @param theParams the list of parameters
  * @author Barry Becker
  */
abstract class AbstractParameterArray private[parameter](theParams: Array[Parameter] = Array[Parameter]())
    extends ParameterArray {

  assert(theParams != null)
  var params: Array[Parameter] = theParams

  /** assign a fitness (evaluation value) to this set of parameters */
  private var fitness: Double = 0

  def this(params: List[Parameter]) {
    this(params.toArray)
  }

  override def getSamplePopulationSize: Int = {
    var pop = 1
    assert(params != null)
    for (param <- params) {
      pop *= (if (param.isIntegerOnly) 4 else 12)
    }
    Math.min(AbstractParameterArray.POPULATION_MAX, pop)
  }

  /** @return the number of parameters in the array. */
  override def size: Int = params.length

  /** @return a copy of ourselves.*/
  override def copy: AbstractParameterArray = {
    val newParams = params.map(_.copy)
    val pa = createInstance
    pa.params = newParams
    pa.setFitness(fitness)
    pa
  }

  protected def createInstance: AbstractParameterArray

  /** @return the ith parameter in the array. */
  override def get(i: Int): Parameter = params(i)

  override def getFitness: Double = fitness
  override def setFitness(value: Double): Unit = {
    fitness = value
  }

  override def toString: String = {
    val sb = new StringBuilder("\n")
    for (i <- 0 until size) {
      sb.append("parameter[").append(i).append("] = ").append(get(i).toString)
      sb.append('\n')
    }
    sb.append("fitness = ").append(this.getFitness)
    sb.toString
  }

  /** @return the parameters in a string of Comma Separated Values. */
  override def toCSVString: String = {
    val sb = new StringBuilder("")
    for (i <- 0 until size -1) {
      sb.append(FormatUtil.formatNumber(get(i).getValue)).append(", ")
    }
    sb.append(FormatUtil.formatNumber(get(size - 1).getValue))
    sb.toString
  }

  /** Natural ordering based on the fitness evaluation assigned to this parameter array.
    * @param params the parameter array to compare ourselves too.
    * @return -1 if we are less than params, 1 if greater than params, 0 if equal.
    */
  override def compareTo(params: ParameterArray): Int = {
    val diff = this.getFitness - params.getFitness
    if (diff < 0) return -1
    if (diff > 0) 1
    else 0
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[AbstractParameterArray]

  override def equals(other: Any): Boolean = other match {
    case that: AbstractParameterArray =>
      (that canEqual this) &&
        (params sameElements that.params) &&
        fitness == that.fitness
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(params, fitness)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.common.format.FormatUtil
import com.barrybecker4.optimization.parameter.types.Parameter
import scala.util.Random


object AbstractParameterArray {
  /** Never exceed this amount  */
  private val POPULATION_MAX = 4000
}

/**
  * Represents a 1 dimensional array of parameters. This is the thing to be optimized.
  * @param params the list of parameters
  * @author Barry Becker
  */
abstract class AbstractParameterArray(params: IndexedSeq[Parameter] = IndexedSeq[Parameter](), rnd: Random)
    extends ParameterArray {

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

  /** @return the ith parameter in the array. Consider using apply instead. */
  override def get(i: Int): Parameter = params(i)

  override def toString: String = {
    val sb = new StringBuilder("\n")
    for (i <- 0 until size) {
      sb.append("parameter[").append(i).append("] = ").append(get(i).toString)
      sb.append('\n')
    }
    //sb.append("rnd="+rnd.toString)
    sb.toString
  }

  /** @return the parameters in a string of Comma Separated Values. */
  override def toCSVString: String = {
    val sb = new StringBuilder("")
    for (i <- 0 until size -1) {
      sb.append(FormatUtil.formatNumber(get(i).getValue)).append(", ")
    }
    if (size > 0) sb.append(FormatUtil.formatNumber(get(size - 1).getValue))
    sb.toString
  }
}
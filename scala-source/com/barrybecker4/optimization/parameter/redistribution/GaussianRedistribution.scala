// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.common.math.MathUtil
import com.barrybecker4.common.math.Range
import com.barrybecker4.common.math.function.ArrayFunction
import com.barrybecker4.common.math.function.FunctionInverter
import com.barrybecker4.common.math.function.ErrorFunction
import AbstractRedistributionFunction.verifyInRange


object GaussianRedistribution {
  private val NUM_MAP_VALUES = 10
  private val SQRT2 = Math.sqrt(2.0)

  def main(args: Array[String]): Unit = {
    new GaussianRedistribution(0.5, 10.0)
  }
}

/**
  * Convert the uniform distribution to a normal (gaussian) one.
  * @author Barry Becker
  */
class GaussianRedistribution(var mean: Double, var stdDeviation: Double) extends AbstractRedistributionFunction {

  verifyInRange(mean)
  private var errorFunction = new ErrorFunction
  initializeFunction()

  override protected def initializeFunction(): Unit = {
    var functionMap: Array[Double] = null
    val inc = 1.0 / (GaussianRedistribution.NUM_MAP_VALUES - 1)
    val cdfFunction = new Array[Double](GaussianRedistribution.NUM_MAP_VALUES)
    cdfFunction(0) = 0
    var x = 0
    for (index <- 1 until GaussianRedistribution.NUM_MAP_VALUES) {
      x += inc
      val v = cdf(x)
      cdfFunction(index) = v
    }
    val lowMissing = cdf(0)
    val highMissing = 1.0 - cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1)
    System.out.println("lowMissing=" + lowMissing + " highMissing=" + highMissing)
    // reallocate the part that is missing.
    val numMapValsm1 = GaussianRedistribution.NUM_MAP_VALUES - 1

    for (i <- 1 until GaussianRedistribution.NUM_MAP_VALUES) {
      val aliasAllocation = -lowMissing *
        (numMapValsm1 - i).toDouble / numMapValsm1 + highMissing * i.toDouble / numMapValsm1
      cdfFunction(i) += aliasAllocation
      if (cdfFunction(i) > 1.0 && i < GaussianRedistribution.NUM_MAP_VALUES - 1)
        cdfFunction(i) = 1.0 - MathUtil.EPS
    }
    val max = cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1)
    assert(max > 0.9 && max < 1.01)
    cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1) = 1.0
    val xRange = Range(0.0, 1.0)
    val inverter = new FunctionInverter(cdfFunction)
    functionMap = inverter.createInverseFunction(xRange)
    redistributionFunction = new ArrayFunction(functionMap, cdfFunction)
  }

  /**
    * 1/2 (1 + erf((x-mean)/(SQRT2 *stdDeviation))
    * @param x x coordinate position.
    * @return cdf cumulative distribution function value
    */
  private def cdf(x: Double) = {
    val denom = GaussianRedistribution.SQRT2 * stdDeviation
    val xx = (Math.min(1.0, x) - mean) / denom
    val erf = errorFunction.getValue(xx)
    0.5 * (1.0 + erf)
  }
}

// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.Range
import com.barrybecker4.math.function.ArrayFunction
import com.barrybecker4.math.function.FunctionInverter
import com.barrybecker4.math.function.ErrorFunction
import RedistributionFunction.verifyInRange


object GaussianRedistribution {
  private val NUM_MAP_VALUES = 10
  private val SQRT2 = Math.sqrt(2.0)
}

/**
  * Convert the uniform distribution to a normal (gaussian) one.
  * @author Barry Becker
  */
case class GaussianRedistribution(var mean: Double, var stdDeviation: Double) extends RedistributionFunction {

  verifyInRange(mean)
  private val errorFunction = new ErrorFunction
  initializeFunction()

  override protected def initializeFunction(): Unit = {
    val cdfFunction = buildInitialCdfSamples()
    val lowMissing = cdf(0)
    val highMissing = 1.0 - cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1)
    redistributeTailMass(cdfFunction, lowMissing, highMissing)
    val max = cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1)
    assert(max > 0.9 && max < 1.01)
    cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1) = 1.0
    val xRange = Range(0.0, 1.0)
    val inverter = new FunctionInverter(cdfFunction)
    val functionMap = inverter.createInverseFunction(xRange)
    redistributionFunction = new ArrayFunction(functionMap, cdfFunction)
  }

  /** Discrete samples of the CDF at uniform x in (0, 1], index 0 fixed at 0. */
  private def buildInitialCdfSamples(): Array[Double] = {
    val n = GaussianRedistribution.NUM_MAP_VALUES
    val inc: Double = 1.0 / (n - 1)
    val cdfFunction = new Array[Double](n)
    cdfFunction(0) = 0
    var x: Double = 0
    for (index <- 1 until n) {
      x += inc
      cdfFunction(index) = cdf(x)
    }
    cdfFunction
  }

  /** Spread missing tail mass across interior CDF samples so the table stays numerically usable. */
  private def redistributeTailMass(cdfFunction: Array[Double], lowMissing: Double, highMissing: Double): Unit = {
    val n = GaussianRedistribution.NUM_MAP_VALUES
    val numMapValsm1 = n - 1
    for (i <- 1 until n) {
      val aliasAllocation =
        -lowMissing * (numMapValsm1 - i).toDouble / numMapValsm1 + highMissing * i.toDouble / numMapValsm1
      cdfFunction(i) += aliasAllocation
      if (cdfFunction(i) > 1.0 && i < n - 1)
        cdfFunction(i) = 1.0 - MathUtil.EPS
    }
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

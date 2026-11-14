// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.Range
import com.barrybecker4.math.function.ArrayFunction
import com.barrybecker4.math.function.FunctionInverter
import com.barrybecker4.math.function.ErrorFunction
import com.barrybecker4.math.function.InvertibleFunction
import RedistributionFunction.verifyInRange


object GaussianRedistribution {
  /** CDF sample count on [0,1]; higher improves narrow-σ invertibility (was 10). */
  private[redistribution] val NUM_MAP_VALUES = 64
  private val SQRT2 = Math.sqrt(2.0)
}

/**
  * Convert the uniform distribution to a normal (gaussian) one.
  * @author Barry Becker
  */
case class GaussianRedistribution(mean: Double, stdDeviation: Double) extends RedistributionFunction {

  verifyInRange(mean)
  private val errorFunction = new ErrorFunction
  protected val redistributionFunction: InvertibleFunction = buildRedistributionFunction()

  private def buildRedistributionFunction(): InvertibleFunction = {
    val cdfFunction = buildInitialCdfSamples()
    val lowMissing = cdf(0)
    val highMissing = 1.0 - cdfFunction(GaussianRedistribution.NUM_MAP_VALUES - 1)
    redistributeTailMass(cdfFunction, lowMissing, highMissing)
    enforceMonotonicUnitCdf(cdfFunction)
    val xRange = Range(0.0, 1.0)
    val inverter = new FunctionInverter(cdfFunction)
    val functionMap = inverter.createInverseFunction(xRange)
    new ArrayFunction(functionMap, cdfFunction)
  }

  /**
    * Tail redistribution can produce tiny non-monotonic dips for narrow σ / fine grids.
    * Clamp to a non-decreasing unit-interval CDF before inversion.
    */
  private def enforceMonotonicUnitCdf(cdfFunction: Array[Double]): Unit = {
    cdfFunction(0) = 0.0
    val last = cdfFunction.length - 1
    for (i <- 1 until last) {
      if (cdfFunction(i) < cdfFunction(i - 1)) cdfFunction(i) = cdfFunction(i - 1)
      if (cdfFunction(i) > 1.0 - MathUtil.EPS) cdfFunction(i) = 1.0 - MathUtil.EPS
      if (cdfFunction(i) < 0.0) cdfFunction(i) = 0.0
    }
    cdfFunction(last) = 1.0
    if (cdfFunction(last) < cdfFunction(last - 1))
      cdfFunction(last - 1) = math.min(cdfFunction(last - 1), 1.0 - MathUtil.EPS)
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

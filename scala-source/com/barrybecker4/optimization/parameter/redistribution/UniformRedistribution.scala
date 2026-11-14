// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.redistribution

import com.barrybecker4.math.MathUtil
import com.barrybecker4.math.function.{InvertibleFunction, PiecewiseFunction}
import RedistributionFunction.verifyInRange

import scala.collection.mutable.ArrayBuffer


/**
  * The default is a completely uniform distribution (all values having equal probability).
  * However, to get that, you do not actually need to specify any redistribution function,
  * so this class offers a twist on the concept. You can specify special values that you
  * want to have special probabilities in an otherwise uniform distribution.
  *
  * @param specialValues certain values that are more likely to occur than other regular values
  *                      (must be in increasing order)
  * @param specialValueProbabilities sum of all special value probabilities must be less than or equal to one.
  *                                  If equal to one, no non-special values are ever selected.
  * @author Barry Becker
  */
class UniformRedistribution(
    protected val specialValues: Array[Double],
    protected val specialValueProbabilities: Array[Double]
) extends RedistributionFunction {

  private val len = specialValues.length
  assert(len > 0,
    "must have at least one special value " + "(otherwise you could just use null for the redistribution function)")
  assert(len == specialValueProbabilities.length)

  protected val redistributionFunction: InvertibleFunction = buildPiecewiseFunction()

  /**
    * Build a piecewise CDF map for the special-value spikes.
    * bb4-math 2.1 requires [[PiecewiseFunction]] x samples to be strictly increasing, so
    * coincident endpoint / plateau samples are omitted (or nudged) instead of duplicated.
    * When special probabilities sum to 1 and the last special is below 1.0, a tiny final
    * gap is reserved so the terminal (1, 1) sample can advance past the last plateau
    * (`1.0 + EPS` does not change a Double already at 1.0).
    */
  private def buildPiecewiseFunction(): PiecewiseFunction = {
    val specialProbabilityTotal = getSpecialProbTotal
    val ratio = 1.0 - specialProbabilityTotal
    val reserveFinalGap =
      specialProbabilityTotal >= 1.0 - MathUtil.EPS && specialValues(len - 1) < 1.0 - MathUtil.EPS
    val xValues = ArrayBuffer.empty[Double]
    val yValues = ArrayBuffer.empty[Double]

    def appendPoint(x: Double, y: Double): Unit = {
      if (xValues.isEmpty || x > xValues.last) {
        xValues += x
        yValues += y
      } else if (x < xValues.last || y != yValues.last) {
        // Keep a distinct sample when the computed x would not advance (e.g. special value at 0).
        // MathUtil.EPS is smaller than 1 ulp at 1.0, so use nextUp and fall back to updating y.
        val nudged = Math.nextUp(xValues.last)
        if (nudged <= 1.0) {
          xValues += nudged
          yValues += y
        } else {
          yValues(yValues.length - 1) = y
        }
      }
      // else: exact duplicate of the previous sample — drop it
    }

    appendPoint(0.0, 0.0)
    var lastX = 0.0
    for (i <- 0 until len) {
      verifyInRange(specialValueProbabilities(i))
      val specialValuesm1 = if (i == 0) 0.0 else specialValues(i - 1)
      val xBase = lastX + (specialValues(i) - specialValuesm1) * ratio
      val xStart = if (i > 0) xBase + MathUtil.EPS else xBase
      val xEndRaw = xBase + specialValueProbabilities(i)
      val xEnd =
        if (i == len - 1 && reserveFinalGap) math.min(xEndRaw, 1.0 - MathUtil.EPS_MEDIUM)
        else xEndRaw
      appendPoint(xStart, specialValues(i))
      appendPoint(xEnd, specialValues(i))
      lastX = xEnd
    }
    appendPoint(1.0, 1.0)

    new PiecewiseFunction(xValues.toArray, yValues.toArray)
  }

  private def getSpecialProbTotal = {
    var specialProbabilityTotal: Double = 0
    for (i <- 0 until len) {
      verifyInRange(specialValues(i))
      if (i > 0) assert(specialValues(i) > specialValues(i - 1))
      specialProbabilityTotal += specialValueProbabilities(i)
    }
    //assert (specialProbabilityTotal < 1.0) :
    // "Sum of special probabilities is not less than one. It was " + specialProbabilityTotal;
    verifyInRange(specialProbabilityTotal)
    specialProbabilityTotal
  }
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.projectors

import com.barrybecker4.math.Range
import com.barrybecker4.optimization.parameter.ParameterArray
import javax.vecmath.Point2d


/**
  * This simple projector strategy simply adds the even dimension values together to get an x coordinate,
  * and adds the odd dimension values together to get a y coordinate.
  * For a single-parameter array, Y is taken equal to X so the view has a non-degenerate range and the
  * path is visible in 2D (diagonal). The same mirroring applies if only odd-indexed parameters exist.
  * TODO: create a PCS projector
  *
  * @author Barry Becker
  */
class SimpleProjector extends Projector {
  override def project(params: ParameterArray): Point2d = {
    var xVal: Double = 0
    var yVal: Double = 0
    for (i <- 0 until params.size) {
      val v = params.get(i).getValue
      if (i % 2 == 0) xVal += v
      else yVal += v
    }
    if (!hasEvenIndexedParams(params)) xVal = yVal
    else if (!hasOddIndexedParams(params)) yVal = xVal
    new Point2d(xVal, yVal)
  }

  override def getXRange(params: ParameterArray): Range = {
    val r = findRange(params, 0)
    if (rangeHasPositiveExtent(r)) r
    else findRange(params, 1)
  }

  override def getYRange(params: ParameterArray): Range = {
    val r = findRange(params, 1)
    if (rangeHasPositiveExtent(r)) r
    else findRange(params, 0)
  }

  private def hasEvenIndexedParams(params: ParameterArray): Boolean = params.size >= 1

  private def hasOddIndexedParams(params: ParameterArray): Boolean = params.size >= 2

  private def rangeHasPositiveExtent(r: Range): Boolean = r.getExtent > 0

  private def findRange(params: ParameterArray, modulus: Int) = {
    var min: Double = 0
    var max: Double = 0
    for (i <- 0 until params.size) {
      if (i % 2 == modulus) {
        min += params.get(i).minValue
        max += params.get(i).maxValue
      }
    }
    Range(min, max)
  }
}

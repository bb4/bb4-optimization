// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.projectors

import com.barrybecker4.math.Range
import com.barrybecker4.optimization.parameter.ParameterArray
import javax.vecmath.Point2d


/**
  * This simple projector strategy simply adds the even dimension values together to get an x coordinate,
  * and adds the odd dimension values together to get a y coordinate.
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
    new Point2d(xVal, yVal)
  }

  override def getXRange(params: ParameterArray): Range = findRange(params, 0)

  override def getYRange(params: ParameterArray): Range = findRange(params, 1)

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
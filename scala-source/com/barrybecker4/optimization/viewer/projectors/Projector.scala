// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.projectors

import com.barrybecker4.optimization.parameter.ParameterArray
import javax.vecmath.Point2d
import com.barrybecker4.common.math.Range

/**
  * A method for projecting from high dimensional space to 2d.
  * @author Barry Becker
  */
trait Projector {

  def project(params: ParameterArray): Point2d

  def getXRange(params: ParameterArray): Range

  def getYRange(params: ParameterArray): Range
}

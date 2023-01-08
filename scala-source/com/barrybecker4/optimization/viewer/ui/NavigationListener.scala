// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import javax.vecmath.Point2d


/**
  * Implemented by something that listens for navigation events.
  * @author Barry Becker
  */
trait NavigationListener {

  def pan(offset: Point2d): Unit

  def zoomIn(): Unit

  def zoomOut(): Unit
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.model

import com.barrybecker4.math.Range
import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness
import com.barrybecker4.optimization.viewer.projectors.Projector
import com.barrybecker4.optimization.viewer.ui.NavigationListener
import javax.vecmath.Point2d

import java.awt.Point
import scala.collection.mutable.ArrayBuffer

object PointsList {

  /** zoom in increment */
  private val ZOOM_IN_INCREMENT: Double = 0.2

  /** zoom out by this much */
  private val ZOOM_OUT_INCREMENT: Double = 1.05
}

/**
  * Trajectory and projection state for the optimization viewer.
  * @param rawSolutionPosition where we hope to wind up at.
  * @author Barry Becker
  */
class PointsList(var rawSolutionPosition: Point2d, var edgeSize: Int, var projector: Projector)
    extends NavigationListener {

  private val rawPoints = ArrayBuffer[Point2d]()
  private val paramArrays = ArrayBuffer[ParameterArrayWithFitness]()

  private var rangeX: Option[Range] = None
  private var rangeY: Option[Range] = None

  def getSolutionPosition: Point =
    new Point(getScaledXValue(rawSolutionPosition.x), getScaledYValue(rawSolutionPosition.y))

  def getRawPoint(i: Int): Point2d = rawPoints(i)

  def getParamArrayForPoint(i: Int): ParameterArrayWithFitness = paramArrays(i)

  def getScaledPoint(i: Int): Point = {
    val pt: Point2d = rawPoints(i)
    new Point(getScaledXValue(pt.x), getScaledYValue(pt.y))
  }

  def getScaledRawPoint(x: Double, y: Double): Point =
    new Point(getScaledXValue(x), getScaledYValue(y))

  def size: Int = rawPoints.size

  /** Called whenever the optimizer strategy moves incrementally toward the solution.
    * Does first time initialization of axis ranges.
    */
  def addPoint(params: ParameterArrayWithFitness): Unit = {
    if rangeX.isEmpty then
      rangeX = Some(projector.getXRange(params.pa))
      rangeY = Some(projector.getYRange(params.pa))
    rawPoints += projector.project(params.pa)
    paramArrays += params
  }

  override def pan(offset: Point2d): Unit =
    for
      rx <- rangeX
      ry <- rangeY
    do
      adjustXRange(offset.x * safeExtent(rx))
      adjustYRange(offset.y * safeExtent(ry))

  private def adjustXRange(xOffset: Double): Unit =
    rangeX = rangeX.map(r => Range(r.min + xOffset, r.max + xOffset))

  private def adjustYRange(yOffset: Double): Unit =
    rangeY = rangeY.map(r => Range(r.min + yOffset, r.max + yOffset))

  override def zoomIn(): Unit =
    for
      rx <- rangeX
      ry <- rangeY
    do
      val xOffset: Double = 0.5 * PointsList.ZOOM_IN_INCREMENT * safeExtent(rx)
      val yOffset: Double = 0.5 * PointsList.ZOOM_IN_INCREMENT * safeExtent(ry)
      adjustRanges(xOffset, yOffset)

  override def zoomOut(): Unit =
    for
      rx <- rangeX
      ry <- rangeY
    do
      val xOffset: Double = -0.5 * PointsList.ZOOM_OUT_INCREMENT * safeExtent(rx)
      val yOffset: Double = -0.5 * PointsList.ZOOM_OUT_INCREMENT * safeExtent(ry)
      adjustRanges(xOffset, yOffset)

  private def adjustRanges(xOffset: Double, yOffset: Double): Unit =
    rangeX = rangeX.map(r => Range(r.min + xOffset, r.max - xOffset))
    rangeY = rangeY.map(r => Range(r.min + yOffset, r.max - yOffset))

  private def getScaledXValue(value: Double): Int =
    rangeX match
      case None        => 0
      case Some(range) => (edgeSize * (value - range.min) / safeExtent(range)).toInt

  private def getScaledYValue(value: Double): Int =
    rangeY match
      case None        => 0
      case Some(range) => (edgeSize * (value - range.min) / safeExtent(range)).toInt

  /** Avoid NaN when projector returns a zero-width range. */
  private def safeExtent(r: Range): Double = {
    val e = r.getExtent
    if e > 0 && !e.isNaN && !e.isInfinite then e else 1.0
  }
}

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.model

import java.awt.Point

import com.barrybecker4.math.Range
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.viewer.NavigationListener
import com.barrybecker4.optimization.viewer.projectors.Projector
import javax.vecmath.Point2d

import scala.collection.mutable.ArrayBuffer


object PointsList {

  /** zoom in increment */
  private val ZOOM_IN_INCREMENT: Double = 0.2

  /** zoom out by this much */
  private val ZOOM_OUT_INCREMENT: Double = 1.05
}

/**
  * Panel for showing the optimization visually.
  * @param rawSolutionPosition where we hope to wind up at.
  * @author Barry Becker
  */
class PointsList(var rawSolutionPosition: Point2d, var edgeSize: Int, var projector: Projector)
   extends NavigationListener {

  private var rawPoints = ArrayBuffer[Point2d]()
  private var paramArrays = ArrayBuffer[ParameterArrayWithFitness]()

  private var rangeX: Range = _
  private var rangeY: Range = _

  def getSolutionPosition: Point =
    new Point(getScaledXValue(rawSolutionPosition.x), getScaledYValue(rawSolutionPosition.y))

  def getRawPoint(i: Int): Point2d = rawPoints(i)

  def getParamArrayForPoint(i: Int): ParameterArrayWithFitness = paramArrays(i)

  def getScaledPoint(i: Int): Point = {
    val pt: Point2d = rawPoints(i)
    new Point(getScaledXValue(pt.x), getScaledYValue(pt.y))
  }

  def size: Int = rawPoints.size

  /** Called whenever the optimizer strategy moves incrementally toward the solution.
    * Does first time initialization.
    * @param params the parameter array to add to the list.
    */
  def addPoint(params: ParameterArrayWithFitness): Unit = {
    if (rangeX == null) {
      rangeX = projector.getXRange(params.pa)
      rangeY = projector.getYRange(params.pa)
    }
    rawPoints += projector.project(params.pa)
    paramArrays += params
  }

  override def pan(offset: Point2d): Unit = {
    adjustXRange(offset.x * rangeX.getExtent)
    adjustYRange(offset.y * rangeY.getExtent)
  }

  private def adjustXRange(xOffset: Double): Unit =
    rangeX = Range(rangeX.min + xOffset, rangeX.max + xOffset)

  private def adjustYRange(yOffset: Double): Unit =
    rangeY = Range(rangeY.min + yOffset, rangeY.max + yOffset)

  override def zoomIn(): Unit = {
    val xOffset: Double = 0.5 * PointsList.ZOOM_IN_INCREMENT * rangeX.getExtent
    val yOffset: Double = 0.5 * PointsList.ZOOM_IN_INCREMENT * rangeY.getExtent
    adjustRanges(xOffset, yOffset)
  }

  override def zoomOut(): Unit = {
    val xOffset: Double = -0.5 * PointsList.ZOOM_OUT_INCREMENT * rangeX.getExtent
    val yOffset: Double = -0.5 * PointsList.ZOOM_OUT_INCREMENT * rangeY.getExtent
    adjustRanges(xOffset, yOffset)
  }

  private def adjustRanges(xOffset: Double, yOffset: Double): Unit = {
    rangeX = Range(rangeX.min + xOffset, rangeX.max - xOffset)
    rangeY = Range(rangeY.min + yOffset, rangeY.max - yOffset)
  }

  private def getScaledXValue(value: Double): Int = {
    if (rangeX == null) return 0
    (edgeSize * (value - rangeX.min) / rangeX.getExtent).toInt
  }

  private def getScaledYValue(value: Double): Int = {
    if (rangeY == null) return 0
    (edgeSize * (value - rangeY.min) / rangeY.getExtent).toInt
  }
}
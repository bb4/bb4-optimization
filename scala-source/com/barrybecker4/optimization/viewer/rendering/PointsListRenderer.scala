// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.rendering

import com.barrybecker4.optimization.parameter.ParameterArrayWithFitness
import com.barrybecker4.optimization.viewer.model.PointsList

import java.awt.{BasicStroke, Color, Font, Graphics2D, RenderingHints}
import java.awt.geom.Line2D

object PointsListRenderer {
  private val POINT_DIAMETER = 8
  private val VECTOR_COLOR = new Color(10, 20, 255, 120)
  private val POINT_COLOR = new Color(10, 0, 55)
  private val FINAL_POINT_COLOR = new Color(255, 80, 0)
  private val SOLUTION_COLOR = new Color(220, 0, 0)
  private val TEXT_COLOR = new Color(50, 50, 50)
  private val GRID_COLOR = new Color(200, 200, 210, 80)
  private val LINE_STROKE = new BasicStroke(1.0f)
  private val POINT_STROKE = new BasicStroke(2.0f)
  private val GRID_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, Array(4f, 4f), 0f)
  private val cmap = new FitnessColorMap

  /** Rendering toggles (defaults favor readability for long runs). */
  case class Config(
      showPointLabels: Boolean = false,
      labelEveryN: Int = 5,
      showGrid: Boolean = true
  )
}

/**
  * Draws projected optimization trajectory, optional grid, and fitness-colored halos.
  * @author Barry Becker
  */
class PointsListRenderer {

  def render(points: PointsList, g2: Graphics2D, config: PointsListRenderer.Config,
             viewportWidth: Int, viewportHeight: Int): Unit = {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if config.showGrid then drawGrid(g2, viewportWidth, viewportHeight)
    drawSolution(g2, points.getSolutionPosition)
    val numPoints = points.size
    var i = 1
    while i < numPoints do
      val isLast = i == numPoints - 1
      val showLabel = config.showPointLabels && (isLast || (i % config.labelEveryN == 0))
      drawVector(
        g2,
        points.getScaledPoint(i - 1),
        points.getScaledPoint(i),
        points.getParamArrayForPoint(i),
        isLast,
        showLabel
      )
      i += 1
  }

  private def drawGrid(g2: Graphics2D, w: Int, h: Int): Unit = {
    g2.setStroke(PointsListRenderer.GRID_STROKE)
    g2.setColor(PointsListRenderer.GRID_COLOR)
    val step = 50
    var x = step
    while x < w do
      g2.draw(new Line2D.Double(x, 0, x, h))
      x += step
    var y = step
    while y < h do
      g2.draw(new Line2D.Double(0, y, w, y))
      y += step
  }

  private def drawVector(
      g2: Graphics2D,
      lastPoint: java.awt.Point,
      currentPoint: java.awt.Point,
      params: ParameterArrayWithFitness,
      isLast: Boolean,
      showLabel: Boolean
  ): Unit = {
    g2.setStroke(PointsListRenderer.LINE_STROKE)
    g2.setColor(PointsListRenderer.VECTOR_COLOR)
    g2.drawLine(currentPoint.x, currentPoint.y, lastPoint.x, lastPoint.y)
    g2.setColor(PointsListRenderer.cmap.getColorForValue(params.fitness))
    fillOval(currentPoint, 3 * PointsListRenderer.POINT_DIAMETER, g2)
    g2.setStroke(PointsListRenderer.POINT_STROKE)
    g2.setColor(if isLast then PointsListRenderer.FINAL_POINT_COLOR else PointsListRenderer.POINT_COLOR)
    drawOval(currentPoint, PointsListRenderer.POINT_DIAMETER, g2)
    if showLabel then
      g2.setColor(PointsListRenderer.TEXT_COLOR)
      val label = "(" + params.pa.toCSVString + ")"
      val small = g2.getFont.deriveFont(Font.PLAIN, 10f)
      g2.setFont(small)
      g2.drawString(label, currentPoint.x + 8, currentPoint.y - 4)
  }

  private def drawSolution(g2: Graphics2D, position: java.awt.Point): Unit = {
    g2.setColor(PointsListRenderer.SOLUTION_COLOR)
    g2.setStroke(PointsListRenderer.POINT_STROKE)
    drawOval(position, PointsListRenderer.POINT_DIAMETER - 2, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER + 3, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER + 10, g2)
  }

  private def drawOval(position: java.awt.Point, rad: Int, g2: Graphics2D): Unit =
    g2.drawOval((position.x - rad / 2.0).toInt, (position.y - rad / 2.0).toInt, rad, rad)

  private def fillOval(position: java.awt.Point, rad: Int, g2: Graphics2D): Unit =
    g2.fillOval((position.x - rad / 2.0).toInt, (position.y - rad / 2.0).toInt, rad, rad)
}

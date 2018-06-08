// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer

import java.awt._
import com.barrybecker4.optimization.parameter.ParameterArray
import com.barrybecker4.optimization.viewer.model.PointsList
import javax.vecmath.Point2d


object PointsListRenderer {
  private val POINT_DIAMETER = 8
  private val VECTOR_COLOR = new Color(10, 20, 255, 120)
  private val POINT_COLOR = new Color(10, 0, 55)
  private val FINAL_POINT_COLOR = new Color(255, 80, 0)
  private val SOLUTION_COLOR = new Color(220, 0, 0)
  private val TEXT_COLOR = new Color(50, 50, 50)
  private val LINE_STROKE = new BasicStroke(1.0f)
  private val POINT_STROKE = new BasicStroke(2.0f)
  private val cmap = new FitnessColorMap
}

/**
  * Panel for showing the optimization visually.
  * @author Barry Becker
  */
class PointsListRenderer {

  def render(points: PointsList, g2: Graphics2D): Unit = {
    if (points.getSolutionPosition != null) drawSolution(g2, points.getSolutionPosition)
    val numPoints = points.size
    for (i <- 1 until numPoints) {
      drawVector(g2, points.getScaledPoint(i - 1), points.getScaledPoint(i),
        points.getRawPoint(i), points.getParamArrayForPoint(i), i == (numPoints - 1))
    }
  }

  private def drawVector(g2: Graphics2D, lastPoint: Point, currentPoint: Point, rawPoint: Point2d,
                         params: ParameterArray, isLast: Boolean): Unit = {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setStroke(PointsListRenderer.LINE_STROKE)
    g2.setColor(PointsListRenderer.VECTOR_COLOR)
    g2.drawLine(currentPoint.x, currentPoint.y, lastPoint.x, lastPoint.y)
    g2.setColor(PointsListRenderer.cmap.getColorForValue(params.getFitness))
    fillOval(currentPoint, 3 * PointsListRenderer.POINT_DIAMETER, g2)
    g2.setStroke(PointsListRenderer.POINT_STROKE)
    g2.setColor(if (isLast) PointsListRenderer.FINAL_POINT_COLOR
    else PointsListRenderer.POINT_COLOR)
    drawOval(currentPoint, PointsListRenderer.POINT_DIAMETER, g2)
    g2.setColor(PointsListRenderer.TEXT_COLOR)
    //String label = "(" + FormatUtil.formatNumber(rawPoint.x) + ", " + FormatUtil.formatNumber(rawPoint.y) + ")";
    val label = "(" + params.toCSVString + ")"
    g2.drawString(label, currentPoint.x - 10 - 5 * label.length, currentPoint.y + 12)
  }

  private def drawSolution(g2: Graphics2D, position: Point): Unit = {
    g2.setColor(PointsListRenderer.SOLUTION_COLOR)
    g2.setStroke(PointsListRenderer.POINT_STROKE)
    drawOval(position, PointsListRenderer.POINT_DIAMETER - 2, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER + 3, g2)
    drawOval(position, PointsListRenderer.POINT_DIAMETER + 10, g2)
  }

  private def drawOval(position: Point, rad: Int, g2: Graphics2D): Unit = {
    g2.drawOval((position.x - rad / 2.0).toInt, (position.y - rad / 2.0).toInt, rad, rad)
  }

  private def fillOval(position: Point, rad: Int, g2: Graphics2D): Unit = {
    g2.fillOval((position.x - rad / 2.0).toInt, (position.y - rad / 2.0).toInt, rad, rad)
  }
}
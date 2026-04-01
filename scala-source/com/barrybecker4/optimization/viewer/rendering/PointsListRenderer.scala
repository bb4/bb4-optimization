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
  private val CONTOUR_STROKE = new BasicStroke(1.0f)
  private val cmap = new FitnessColorMap

  /** Rendering toggles (defaults favor readability for long runs). */
  case class Config(
      showPointLabels: Boolean = false,
      labelEveryN: Int = 5,
      showGrid: Boolean = true,
      showContours: Boolean = true,
      showHeatmap: Boolean = false
  )
}

/**
  * Draws projected optimization trajectory, optional grid, and fitness-colored halos.
  * @author Barry Becker
  */
class PointsListRenderer {

  def render(points: PointsList, g2: Graphics2D, config: PointsListRenderer.Config,
             viewportWidth: Int, viewportHeight: Int,
             topologyField: Option[ProjectedTopologyField] = None): Unit = {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    if config.showGrid then drawGrid(g2, viewportWidth, viewportHeight)
    topologyField.foreach { field =>
      if config.showHeatmap then drawHeatmap(g2, points, field)
      if config.showContours then drawContours(g2, points, field)
    }
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

  private def drawHeatmap(g2: Graphics2D, points: PointsList, field: ProjectedTopologyField): Unit = {
    var y = 0
    while y < field.yBins - 1 do
      var x = 0
      while x < field.xBins - 1 do
        val i = field.idx(x, y)
        val v00 = field.normalizedValues(i)
        val v10 = field.normalizedValues(field.idx(x + 1, y))
        val v01 = field.normalizedValues(field.idx(x, y + 1))
        val v11 = field.normalizedValues(field.idx(x + 1, y + 1))
        val v = 0.25 * (v00 + v10 + v01 + v11)
        val p0 = points.getScaledRawPoint(field.rawX(x), field.rawY(y))
        val p1 = points.getScaledRawPoint(field.rawX(x + 1), field.rawY(y + 1))
        val left = math.min(p0.x, p1.x)
        val top = math.min(p0.y, p1.y)
        val w = math.max(1, math.abs(p1.x - p0.x))
        val h = math.max(1, math.abs(p1.y - p0.y))
        g2.setColor(colorForNormalized(v, 55))
        g2.fillRect(left, top, w, h)
        x += 1
      y += 1
  }

  private def drawContours(g2: Graphics2D, points: PointsList, field: ProjectedTopologyField): Unit = {
    g2.setStroke(PointsListRenderer.CONTOUR_STROKE)
    field.contourLevels.foreach { level =>
      val alpha = (65 + (90 * level)).toInt
      g2.setColor(new Color(20, 30, 50, math.min(180, alpha)))
      var y = 0
      while y < field.yBins - 1 do
        var x = 0
        while x < field.xBins - 1 do
          drawContourInCell(g2, points, field, x, y, level)
          x += 1
        y += 1
    }
  }

  private def drawContourInCell(
      g2: Graphics2D,
      points: PointsList,
      field: ProjectedTopologyField,
      x: Int,
      y: Int,
      level: Double
  ): Unit = {
    val p00 = points.getScaledRawPoint(field.rawX(x), field.rawY(y))
    val p10 = points.getScaledRawPoint(field.rawX(x + 1), field.rawY(y))
    val p11 = points.getScaledRawPoint(field.rawX(x + 1), field.rawY(y + 1))
    val p01 = points.getScaledRawPoint(field.rawX(x), field.rawY(y + 1))
    val v00 = field.normalizedValues(field.idx(x, y))
    val v10 = field.normalizedValues(field.idx(x + 1, y))
    val v11 = field.normalizedValues(field.idx(x + 1, y + 1))
    val v01 = field.normalizedValues(field.idx(x, y + 1))

    val intersections = collection.mutable.ArrayBuffer[(Double, Double)]()
    addEdgeIntersection(intersections, p00.x, p00.y, v00, p10.x, p10.y, v10, level)
    addEdgeIntersection(intersections, p10.x, p10.y, v10, p11.x, p11.y, v11, level)
    addEdgeIntersection(intersections, p11.x, p11.y, v11, p01.x, p01.y, v01, level)
    addEdgeIntersection(intersections, p01.x, p01.y, v01, p00.x, p00.y, v00, level)

    if intersections.length >= 2 then
      val pts = intersections.take(4)
      if pts.length >= 2 then
        g2.draw(new Line2D.Double(pts(0)._1, pts(0)._2, pts(1)._1, pts(1)._2))
      if pts.length >= 4 then
        g2.draw(new Line2D.Double(pts(2)._1, pts(2)._2, pts(3)._1, pts(3)._2))
  }

  private def addEdgeIntersection(
      out: collection.mutable.ArrayBuffer[(Double, Double)],
      x1: Double,
      y1: Double,
      v1: Double,
      x2: Double,
      y2: Double,
      v2: Double,
      level: Double
  ): Unit = {
    val crossed = (v1 <= level && v2 > level) || (v2 <= level && v1 > level)
    if crossed then
      val t = if math.abs(v2 - v1) < 1.0e-9 then 0.5 else (level - v1) / (v2 - v1)
      val px = x1 + t * (x2 - x1)
      val py = y1 + t * (y2 - y1)
      out += ((px, py))
  }

  private def colorForNormalized(value: Double, alpha: Int): Color = {
    val v = math.max(0.0, math.min(1.0, value))
    val hue = (0.66 - 0.66 * v).toFloat
    val base = Color.getHSBColor(hue, 0.75f, 1.0f)
    new Color(base.getRed, base.getGreen, base.getBlue, math.max(0, math.min(255, alpha)))
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

// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import com.barrybecker4.optimization.viewer.model.PointsList
import com.barrybecker4.optimization.viewer.projectors.SimpleProjector
import com.barrybecker4.optimization.viewer.rendering.PointsListRenderer
import com.barrybecker4.optimization.viewer.ui.{OptimizationViewable, OptimizerEvalPanel}
import com.barrybecker4.optimization.{OptimizationListener, Optimizer}

import java.awt.*
import java.awt.event.{MouseEvent, MouseListener, MouseMotionListener}
import javax.swing.JPanel
import javax.vecmath.Point2d


object OptimizerEvalPanel {
  private val EDGE_SIZE = 1000
  private[viewer] val SIZE = new Dimension(EDGE_SIZE, EDGE_SIZE)
  private val BG_COLOR = new Color(240, 245, 250)
}

/**
  * Panel for showing the optimization visually.
  * To zoom, click buttons at the top.
  * To pan, simply click and drag.
  * @author Barry Becker
  */
class OptimizerEvalPanel[P <: ParameterArray]() extends JPanel
    with OptimizationListener[P] with OptimizationViewable[P] with MouseListener with MouseMotionListener {

  private val renderer = new PointsListRenderer
  private val projector = new SimpleProjector
  private var pointsList: PointsList[P] = _
  private var dragStartPosition: Point = _

  this.setPreferredSize(OptimizerEvalPanel.SIZE)
  this.addMouseListener(this)
  this.addMouseMotionListener(this)

  def doTest(optType: OptimizationStrategyType[P], optimizer: Optimizer[P],
             solutionPosition: Point2d, initialGuess: P, fitnessRange: Double): Unit = {
    pointsList = new PointsList(solutionPosition, OptimizerEvalPanel.EDGE_SIZE, projector)
    var solution: ParameterArrayWithFitness[P] = null
    try
      solution = optimizer.doOptimization(optType, initialGuess, fitnessRange)
    catch {
      case e: AbstractMethodError =>
        // allow continuing if the strategy has simply not been implemented yet.
        e.printStackTrace()
    }
    this.repaint()
    println("\n************************************************************************")
    println("The solution to the (" + optimizer.optimizee.getName + ") Polynomial Test Problem using " + optType + " is :\n" + solution)
    println("Which evaluates to: " + solution.fitness)
    println("We expected to get exactly p1 = " + solutionPosition.x + " and p2 = " + solutionPosition.y)
  }

  /** Called whenever the optimizer strategy moves incrementally toward the solution.
    * @param params we assume there is only two.
    */
  override def optimizerChanged(params: ParameterArrayWithFitness[P]): Unit =
    pointsList.addPoint(params)

  /** Show the optimization results in the ui. */
  override def showOptimization(strategy: OptimizationStrategyType[P], testProblem: OptimizeeProblem[P], logFile: String): Unit = {
    val params = testProblem.getExactSolution
    // have strategy for projecting n-dimensions down to two.
    val solutionPosition = projector.project(params.pa)
    val optimizer = new Optimizer(testProblem, Some(logFile))
    optimizer.setListener(this)
    doTest(strategy, optimizer, solutionPosition, testProblem.getInitialGuess, testProblem.getFitnessRange)
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponents(g)
    val g2 = g.asInstanceOf[Graphics2D]
    val dim: Dimension = this.getSize
    g2.setColor(OptimizerEvalPanel.BG_COLOR)
    g2.fillRect(0, 0, dim.getWidth.toInt, dim.getHeight.toInt)
    renderer.render(pointsList.asInstanceOf[PointsList[Nothing]], g2)
  }

  override def pan(offset: Point2d): Unit = {
    pointsList.pan(offset)
    repaint()
  }

  override def zoomIn(): Unit = {
    pointsList.zoomIn()
    repaint()
  }

  override def zoomOut(): Unit = {
    pointsList.zoomOut()
    repaint()
  }

  override def mouseDragged(e: MouseEvent): Unit = doPan(e.getPoint)
  override def mouseMoved(e: MouseEvent): Unit = {}
  override def mouseClicked(e: MouseEvent): Unit = {}

  override def mousePressed(e: MouseEvent): Unit = {
    dragStartPosition = e.getPoint
    println("mouse pressed at " + dragStartPosition)
  }

  override def mouseReleased(e: MouseEvent): Unit = doPan(e.getPoint)
  override def mouseEntered(e: MouseEvent): Unit = {}
  override def mouseExited(e: MouseEvent): Unit = {}

  private def doPan(currentPos: Point): Unit = {
    if (!(dragStartPosition == currentPos)) {
      val xOffset = (dragStartPosition.getX - currentPos.getX) / getWidth
      val yOffset = (dragStartPosition.getY - currentPos.getY) / getHeight
      val offset = new Point2d(xOffset, yOffset)
      pointsList.pan(offset)
      this.repaint()
    }
    dragStartPosition = currentPos
  }
}
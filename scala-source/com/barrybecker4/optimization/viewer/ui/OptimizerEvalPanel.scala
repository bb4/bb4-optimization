// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType
import com.barrybecker4.optimization.viewer.model.PointsList
import com.barrybecker4.optimization.viewer.projectors.SimpleProjector
import com.barrybecker4.optimization.viewer.rendering.{PointsListRenderer, ProjectedTopologyField, TopologyCacheKey}
import com.barrybecker4.optimization.{OptimizationListener, Optimizer}

import java.awt.{Color, Dimension, Graphics, Graphics2D, Point}
import java.awt.event.{MouseEvent, MouseListener, MouseMotionListener}
import java.util.concurrent.ExecutionException
import javax.swing.{JComponent, JPanel, SwingUtilities, SwingWorker}
import javax.vecmath.Point2d

import scala.jdk.CollectionConverters.*

object OptimizerEvalPanel {
  private val EDGE_SIZE = 1000
  private[viewer] val SIZE: Dimension = new Dimension(EDGE_SIZE, EDGE_SIZE)
  private val BG_COLOR = new Color(240, 245, 250)
}

private case class OptimizationRunContext(
    optimizer: Optimizer,
    solutionPosition: Point2d,
    initialGuess: ParameterArray,
    fitnessRange: Double
)

/**
  * Panel for showing the optimization visually.
  * To zoom, use toolbar buttons. To pan, click and drag.
  * Optimization runs on a background thread; UI updates and painting occur on the EDT.
  * @author Barry Becker
  */
class OptimizerEvalPanel() extends JPanel
    with OptimizationListener
    with OptimizationViewable
    with MouseListener
    with MouseMotionListener {

  private val renderer = new PointsListRenderer
  private val projector = new SimpleProjector
  private val topologyXBins = 80
  private val topologyYBins = 80
  private var pointsListOpt: Option[PointsList] = None
  private var topologyFieldOpt: Option[ProjectedTopologyField] = None
  private var topologyCache = Map.empty[TopologyCacheKey, ProjectedTopologyField]
  private var dragStart: Option[Point] = None
  private var currentWorker: Option[OptimizationSwingWorker] = None
  private var renderConfig = PointsListRenderer.Config()

  private var statusSink: String => Unit = _ => ()
  private var runStateListener: Boolean => Unit = _ => ()

  setPreferredSize(OptimizerEvalPanel.SIZE)
  addMouseListener(this)
  addMouseMotionListener(this)

  override def configureUiHooks(status: String => Unit, onRunStateChanged: Boolean => Unit): Unit = {
    statusSink = status
    runStateListener = onRunStateChanged
  }

  override def cancelCurrentOptimization(): Unit =
    currentWorker.foreach(_.cancel(true))

  override def setShowPointLabels(enabled: Boolean): Unit = {
    renderConfig = renderConfig.copy(showPointLabels = enabled)
    repaint()
  }

  override def setShowGrid(enabled: Boolean): Unit = {
    renderConfig = renderConfig.copy(showGrid = enabled)
    repaint()
  }

  override def setShowContours(enabled: Boolean): Unit = {
    renderConfig = renderConfig.copy(showContours = enabled)
    repaint()
  }

  override def setShowHeatmap(enabled: Boolean): Unit = {
    renderConfig = renderConfig.copy(showHeatmap = enabled)
    repaint()
  }

  /** Unused: progress is delivered via `javax.swing.SwingWorker` publish/process. */
  override def optimizerChanged(params: ParameterArrayWithFitness): Unit = ()

  override def showOptimization(strategy: OptimizationStrategyType, testProblem: OptimizeeProblem, logFile: String): Unit =
    if SwingUtilities.isEventDispatchThread then startOptimizationRun(strategy, testProblem, logFile)
    else SwingUtilities.invokeLater(() => startOptimizationRun(strategy, testProblem, logFile))

  private def startOptimizationRun(strategy: OptimizationStrategyType, testProblem: OptimizeeProblem, logFile: String): Unit = {
    runStateListener(true)
    updateStatus("Running…")
    currentWorker.foreach(_.cancel(true))

    val ctx = prepareRunContext(testProblem, logFile)
    topologyFieldOpt = buildTopologyField(testProblem, ctx.initialGuess)
    val pointsList = new PointsList(ctx.solutionPosition, OptimizerEvalPanel.EDGE_SIZE, projector)
    pointsListOpt = Some(pointsList)

    val worker = new OptimizationSwingWorker(
      strategy = strategy,
      ctx = ctx,
      pointsList = pointsList,
      paintTarget = this,
      onComplete = outcome => {
        applyOptimizationOutcome(testProblem, strategy, ctx.solutionPosition, outcome)
        runStateListener(false)
        currentWorker = None
      }
    )

    currentWorker = Some(worker)
    worker.execute()
  }

  private def prepareRunContext(testProblem: OptimizeeProblem, logFile: String): OptimizationRunContext = {
    val params = testProblem.getExactSolution
    val solutionPosition = projector.project(params.pa)
    val optimizer = new Optimizer(testProblem, Some(logFile))
    OptimizationRunContext(
      optimizer = optimizer,
      solutionPosition = solutionPosition,
      initialGuess = testProblem.getInitialGuess,
      fitnessRange = testProblem.getFitnessRange
    )
  }

  private def applyOptimizationOutcome(
      testProblem: OptimizeeProblem,
      strategy: OptimizationStrategyType,
      solutionPosition: Point2d,
      outcome: (Boolean, Option[ParameterArrayWithFitness], Option[Throwable])
  ): Unit =
    val (cancelled, solution, error) = outcome
    if cancelled then updateStatus("Cancelled.")
    else if error.isDefined then reportFailure(error.get, strategy)
    else if solution.isEmpty then reportIncomplete(strategy)
    else reportSuccess(testProblem, strategy, solutionPosition, solution.get)

  private def reportFailure(t: Throwable, strategy: OptimizationStrategyType): Unit =
    val msg = t.getMessage match
      case null | "" => t.getClass.getSimpleName
      case m         => m
    updateStatus(s"Failed ($strategy): $msg")

  private def reportIncomplete(strategy: OptimizationStrategyType): Unit =
    updateStatus(
      s"Optimization did not complete ($strategy). The strategy may be incompatible with this problem type."
    )

  private def reportSuccess(
      testProblem: OptimizeeProblem,
      strategy: OptimizationStrategyType,
      solutionPosition: Point2d,
      solution: ParameterArrayWithFitness
  ): Unit =
    updateStatus(
      s"Completed ($strategy) on ${testProblem.getName}: fitness=${solution.fitness} — " +
        s"target projection (${solutionPosition.x}, ${solutionPosition.y})"
    )

  private def updateStatus(msg: String): Unit =
    if SwingUtilities.isEventDispatchThread then statusSink(msg)
    else SwingUtilities.invokeLater(() => statusSink(msg))

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    val g2 = g.asInstanceOf[Graphics2D]
    val dim = getSize
    g2.setColor(OptimizerEvalPanel.BG_COLOR)
    g2.fillRect(0, 0, dim.width, dim.height)
    pointsListOpt.foreach { pl =>
      renderer.render(pl, g2, renderConfig, dim.width, dim.height, topologyFieldOpt)
    }
  }

  private def buildTopologyField(problem: OptimizeeProblem, params: ParameterArray): Option[ProjectedTopologyField] =
    params match
      case npa: NumericParameterArray =>
        try
          val key = ProjectedTopologyField.cacheKey(problem, npa, topologyXBins, topologyYBins)
          topologyCache.get(key) match
            case Some(field) => Some(field)
            case None =>
              val field = ProjectedTopologyField.sample(problem, projector, npa, topologyXBins, topologyYBins)
              topologyCache += (key -> field)
              Some(field)
        catch
          case _: Throwable => None
      case _ => None

  override def pan(offset: Point2d): Unit =
    pointsListOpt.foreach { pl =>
      pl.pan(offset)
      repaint()
    }

  override def zoomIn(): Unit =
    pointsListOpt.foreach { pl =>
      pl.zoomIn()
      repaint()
    }

  override def zoomOut(): Unit =
    pointsListOpt.foreach { pl =>
      pl.zoomOut()
      repaint()
    }

  override def mouseDragged(e: MouseEvent): Unit = doPan(e.getPoint)
  override def mouseMoved(e: MouseEvent): Unit = ()
  override def mouseClicked(e: MouseEvent): Unit = ()

  override def mousePressed(e: MouseEvent): Unit =
    dragStart = Some(e.getPoint)

  override def mouseReleased(e: MouseEvent): Unit = doPan(e.getPoint)
  override def mouseEntered(e: MouseEvent): Unit = ()
  override def mouseExited(e: MouseEvent): Unit = ()

  private def doPan(currentPos: Point): Unit =
    dragStart.foreach { start =>
      if start != currentPos then
        pointsListOpt.foreach { pl =>
          val w = math.max(1, getWidth)
          val h = math.max(1, getHeight)
          val xOffset = (start.getX - currentPos.getX) / w
          val yOffset = (start.getY - currentPos.getY) / h
          pl.pan(new Point2d(xOffset, yOffset))
          repaint()
        }
      dragStart = Some(currentPos)
    }
}

/** Runs [[Optimizer.doOptimization]] off the EDT and streams intermediate points via `publish`/`process`. */
private class OptimizationSwingWorker(
    strategy: OptimizationStrategyType,
    ctx: OptimizationRunContext,
    pointsList: PointsList,
    paintTarget: JComponent,
    onComplete: ((Boolean, Option[ParameterArrayWithFitness], Option[Throwable])) => Unit
) extends SwingWorker[Option[ParameterArrayWithFitness], ParameterArrayWithFitness] {

  override def doInBackground(): Option[ParameterArrayWithFitness] = {
    ctx.optimizer.setListener(p => publish(p))
    try Some(ctx.optimizer.doOptimization(strategy, ctx.initialGuess, ctx.fitnessRange))
    catch
      case e: AbstractMethodError       => logFailure(e); None
      case e: IllegalArgumentException => logFailure(e); None
  }

  private def logFailure(e: Throwable): Unit =
    e.printStackTrace()

  override def process(chunks: java.util.List[ParameterArrayWithFitness]): Unit = {
    chunks.asScala.foreach(pointsList.addPoint)
    paintTarget.repaint()
  }

  override def done(): Unit =
    val outcome =
      if isCancelled then (true, None, None)
      else
        try (false, get(), None)
        catch
          case e: ExecutionException =>
            (false, None, Option(e.getCause))
          case e: InterruptedException =>
            Thread.currentThread().interrupt()
            (false, None, Some(e))
    SwingUtilities.invokeLater(() => onComplete(outcome))
}

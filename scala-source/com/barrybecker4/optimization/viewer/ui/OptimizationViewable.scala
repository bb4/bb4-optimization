// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer.ui

import com.barrybecker4.optimization.OptimizationListener
import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

/**
  * Classes that can show a visualization of an optimization should implement this interface.
  * @author Barry Becker
  */
trait OptimizationViewable extends OptimizationListener with NavigationListener {

  def showOptimization(strategy: OptimizationStrategyType,
                       testProblem: OptimizeeProblem, logFile: String): Unit

  /** Best-effort cancel for a run started on a background worker (may not stop the strategy immediately). */
  def cancelCurrentOptimization(): Unit = ()

  /** Wire status text and run-state callbacks (e.g. enable/disable chrome while a run is active). */
  def configureUiHooks(status: String => Unit, onRunStateChanged: Boolean => Unit): Unit = ()

  def setShowPointLabels(enabled: Boolean): Unit = ()

  def setShowGrid(enabled: Boolean): Unit = ()
}

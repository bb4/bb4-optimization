// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.{Logger, OptimizationDiagnostics, OptimizationListener}
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

/**
  * Abstract base class for Optimization strategy.
  * This and derived classes uses the strategy design pattern.
  * @param optimizee The thing to be optimized
  * @author Barry Becker
  */
abstract class OptimizationStrategy(var optimizee: Optimizee) {

  private var fileLogger: Option[Logger] = None
  protected var diagnostics: OptimizationDiagnostics = OptimizationDiagnostics.Silence
  /** listen for optimization changed events. useful for debugging.  */
  protected var listener: Option[OptimizationListener] = None
  /** Best parameters observed during this run (via [[notifyOfChange]]). */
  private var runBest: Option[ParameterArrayWithFitness] = None

  /** @param logger the file that will record the results */
  def setLogger(logger: Logger): Unit = {
    this.fileLogger = Some(logger)
  }

  /** Whether iteration logging to a file is active (set via [[setLogger]]). */
  protected final def isLoggingToFile: Boolean = fileLogger.isDefined

  /** Console diagnostics when `verbose` is true; off by default. */
  def setVerbose(verbose: Boolean): Unit = {
    diagnostics = if (verbose) new OptimizationDiagnostics.Console(true) else OptimizationDiagnostics.Silence
  }

  def setDiagnostics(d: OptimizationDiagnostics): Unit = {
    diagnostics = d
  }

  protected def trace(msg: => String): Unit = diagnostics.trace(msg)

  protected def log(iteration: Int, params: ParameterArrayWithFitness,
                    jumpSize: Double, deltaFitness: Double,
                    msg: String): Unit = {
    fileLogger.foreach(_.write(iteration, params.fitness, jumpSize, deltaFitness, params.pa, msg))
  }

  /** Best point seen so far this run, if any. */
  def bestSoFar: Option[ParameterArrayWithFitness] = runBest

  /**
    * Copy logger, listener, and diagnostics onto a nested strategy so composite runs
    * (e.g. global hill climbing) share one configuration.
    */
  protected def copyRunConfigurationTo(child: OptimizationStrategy): Unit = {
    fileLogger.foreach(child.setLogger)
    listener.foreach(child.setListener)
    child.setDiagnostics(diagnostics)
  }

  /** @param initialParams the initial guess at the solution.
    * @param fitnessRange  the approximate absolute value of the fitnessRange.
    * @return optimized parameters.
    */
  def doOptimization(initialParams: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness

  def setListener(listener: OptimizationListener): Unit = {
    this.listener = Option(listener)
  }

  /**
    * @param currentBest current best parameter set.
    * @return true if the optimal fitness has been reached.
    */
  private[strategy] def isOptimalFitnessReached(currentBest: ParameterArrayWithFitness): Boolean =
    !optimizee.evaluateByComparison && {
      assert(optimizee.getOptimalFitness >= 0)
      currentBest.fitness <= optimizee.getOptimalFitness
    }

  private[strategy] def notifyOfChange(params: ParameterArrayWithFitness): Unit = {
    updateRunBest(params)
    listener.foreach(_.optimizerChanged(params))
  }

  /**
    * The fitness of `candidate`: absolute, or relative to `baseline` when the optimizee evaluates by comparison.
    * NaN is mapped to positive infinity so that it can never look like an improvement or poison a comparison.
    */
  protected final def scoreAgainst(candidate: ParameterArray, baseline: ParameterArray): Double = {
    val fitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(candidate, baseline)
      else optimizee.evaluateFitness(candidate)
    if (fitness.isNaN) Double.PositiveInfinity else fitness
  }

  /** Record `params` as the run champion when it improves on [[runBest]]. */
  protected final def updateRunBest(params: ParameterArrayWithFitness): Unit = {
    if (runBest.forall(_.fitness > params.fitness)) runBest = Some(params)
  }
}

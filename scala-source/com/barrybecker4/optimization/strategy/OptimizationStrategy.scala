// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.Logger
import com.barrybecker4.optimization.OptimizationListener
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.parameter.ParameterArray


/**
  * Abstract base class for Optimization strategy.
  * This and derived classes uses the strategy design pattern.
  * @param optimizee The thing to be optimized
  * @author Barry Becker
  */
abstract class OptimizationStrategy(var optimizee: Optimizee) {

  private var logger: Logger = _
  /** listen for optimization changed events. useful for debugging.  */
  protected var listener: OptimizationListener = _

  /** @param logger the file that will record the results */
  def setLogger(logger: Logger): Unit = {
    this.logger = logger
  }

  protected def log(iteration: Int, fitness: Double, jumpSize: Double, deltaFitness: Double,
                    params: ParameterArray, msg: String): Unit = {
    if (logger != null) logger.write(iteration, fitness, jumpSize, deltaFitness, params, msg)
  }

  /** @param initialParams the initial guess at the solution.
    * @param fitnessRange  the approximate absolute value of the fitnessRange.
    * @return optimized parameters.
    */
  def doOptimization(initialParams: ParameterArray, fitnessRange: Double): ParameterArray

  def setListener(listener: OptimizationListener): Unit = {
    this.listener = listener
  }

  /**
    * @param currentBest current best parameter set.
    * @return true if the optimal fitness has been reached.
    */
  private[strategy] def isOptimalFitnessReached(currentBest: ParameterArray) = {
    var optimalFitnessReached = false
    if (!optimizee.evaluateByComparison) {
      assert(optimizee.getOptimalFitness >= 0)
      optimalFitnessReached = currentBest.getFitness <= optimizee.getOptimalFitness
    }
    optimalFitnessReached
  }

  private[strategy] def notifyOfChange(params: ParameterArray): Unit = {
    if (listener != null) listener.optimizerChanged(params)
  }
}

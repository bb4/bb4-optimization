// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.optimizee.{BudgetedOptimizee, EvaluationBudgetExceededException, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}
import com.barrybecker4.optimization.strategy.OptimizationStrategyType

import scala.util.Random


/**
  * Uses a specified optimization strategy to optimize something (the optimizee).
  * @see OptimizationStrategyType for a list of the possible algorithms.
  *
  * Uses the delegation design pattern rather than inheritance so that it can be reused across many classes.
  * It abstracts the concept of optimization, and as a result makes it easy to work on independently.
  * For example, this library is used to optimize the motion of the snake in com.barrybecker4.snake (bb4-simulations),
  * the firing of a trebuchet (in bb4-simulations), and solve puzzles efficiently in bb4-puzzles.
  *
  * This class also acts as a facade to the optimization package. The use of this package
  * really does not need to directly construct or use the different optimization strategy classes.
  *
  * Details of the optimization algorithms can be found in
  *    How To Solve It: Modern Heuristics  by Michaelwicz and Fogel
  *
  * @param optimizee the thing to be optimized
  * @param optimizationLogFile (optional) used to log info as optimization proceeds
  * @author Barry Becker
  */
class Optimizer(val optimizee: Optimizee, optimizationLogFile: Option[String] = None) {

  protected var listener: Option[OptimizationListener] = None
  private val logger: Option[Logger] = optimizationLogFile.map(new Logger(_))

  /** Constructs an optimization strategy object of the specified type and run it.
    *
    * @param optimizationType the type of search to perform
    * @param params           the initial guess at the solution; also defines the bounds of the search space
    * @param fitnessRange     approximate scale of the objective (e.g. max − min fitness over the domain of interest).
    *                         Used by strategies that need a temperature or stopping threshold in the same units as
    *                         fitness: e.g. [[com.barrybecker4.optimization.strategy.SimulatedAnnealingStrategy]]
    *                         scales initial temperature from `fitnessRange / 10`, and
    *                         [[com.barrybecker4.optimization.strategy.GeneticSearchStrategy]] derives improvement
    *                         epsilon from `fitnessRange / 100_000_000`. It need not be exact; order-of-magnitude is enough.
    * @param rnd              random source for stochastic strategies
    * @param maxEvaluations   if set, wraps the optimizee so each call to `evaluateFitness` or `compareFitness`
    *                         counts toward a budget; when exceeded, returns best-so-far with
    *                         [[OptimizationStatus.BudgetExhausted]] instead of throwing
    * @param verbose          when true, strategies may print diagnostic traces to the console (off by default)
    * @return [[OptimizationResult]] with the best parameters found and run status
    */
  def doOptimization(optimizationType: OptimizationStrategyType,
                     params: ParameterArray, fitnessRange: Double,
                     rnd: Random = MathUtil.RANDOM,
                     maxEvaluations: Option[Int] = None,
                     verbose: Boolean = false): OptimizationResult = {

    val budgeted: Option[BudgetedOptimizee] = maxEvaluations match {
      case Some(n) if n > 0 => Some(new BudgetedOptimizee(optimizee, n))
      case _ => None
    }
    val target: Optimizee = budgeted.getOrElse(optimizee)

    val optStrategy = optimizationType.getStrategy(target, fitnessRange, rnd)
    logger.foreach { log =>
      log.initialize(params)
      optStrategy.setLogger(log)
    }
    listener.foreach(optStrategy.setListener)
    if (verbose) optStrategy.setVerbose(true)

    try {
      val best = optStrategy.doOptimization(params, fitnessRange)
      val champion = optStrategy.bestSoFar.filter(_.fitness < best.fitness).getOrElse(best)
      if (budgeted.exists(_.wasBudgetExhausted)) {
        OptimizationResult(
          champion,
          OptimizationStatus.BudgetExhausted,
          budgeted.map(_.maxEvaluations)
        )
      } else {
        val status =
          if (!optimizee.evaluateByComparison && champion.fitness <= optimizee.getOptimalFitness)
            OptimizationStatus.OptimalReached
          else OptimizationStatus.Completed
        OptimizationResult(champion, status, budgeted.map(_.evaluationsUsed))
      }
    } catch {
      case e: EvaluationBudgetExceededException =>
        val fallback = ParameterArrayWithFitness(params, Double.MaxValue)
        val best = optStrategy.bestSoFar.getOrElse(fallback)
        OptimizationResult(best, OptimizationStatus.BudgetExhausted, Some(e.maxEvaluations))
    }
  }

  def setListener(lnr: OptimizationListener): Unit = {
    listener = Option(lnr)
  }
}

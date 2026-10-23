// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.hillclimbing

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.*
import com.barrybecker4.optimization.strategy.{CandidateSet, OptimizationStrategy}
import com.barrybecker4.optimization.strategy.gradient.{DiscreteImprovementFinder, Improvement, ImprovementFinder, NumericImprovementFinder}

import scala.collection.mutable


/**
  * Hill climbing optimization strategy.
  * Also known as gradient descent. The latter is more accurate in this case because we are trying to find a minimum.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class HillClimbingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** Finds a local minimum.
    * It is a bit like Newton's method, but in n dimensions.
    * If we make a jump and find that we are worse off than before, we will backtrack and reduce the stepsize so
    * that we can be guaranteed to improve my some amount on every iteration until the incremental improvement
    * is less than the threshold fitness_eps.
    *
    * In comparison mode the returned fitness is relative to `params` (so the initial guess itself scores 0).
    *
    * @param params       the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness =
    climb(startingPoint(params), params)

  /** Climb downhill from a start whose fitness is already known.
    * Used by multistart strategies, which have scored their seeds already.
    *
    * @param start    where to start, with its fitness
    * @param baseline in comparison mode, the array that every fitness is measured against (`start.fitness` must
    *                 be expressed relative to it). Ignored in absolute mode.
    * @return the best point found; in comparison mode its fitness is relative to `baseline`
    */
  def climb(start: ParameterArrayWithFitness, baseline: ParameterArray): ParameterArrayWithFitness = {
    log(0, start, 0.0, 0.0, "initial test")
    notifyOfChange(start)
    if (isOptimalFitnessReached(start)) start
    else {
      // Use cache to avoid repeats in discrete spaces; it has little value in continuous numeric spaces.
      val useCache = isDiscreteProblem(start.pa)
      val cache = newCache(start.pa, useCache)
      descend(createImprovementFinder(start, baseline, useCache), cache)
    }
  }

  /** In comparison mode the start is the baseline, so it scores zero against itself. */
  private def startingPoint(params: ParameterArray): ParameterArrayWithFitness =
    ParameterArrayWithFitness(params, if (optimizee.evaluateByComparison) 0.0 else optimizee.evaluateFitness(params))

  private def newCache(start: ParameterArray, useCache: Boolean): mutable.Set[ParameterArray] = {
    val cache = new CandidateSet()
    if (useCache) cache += start
    cache
  }

  /** Iterate until there is no significant improvement between iterations, i.e. the jumpSize gets too small. */
  private def descend(finder: ImprovementFinder, cache: mutable.Set[ParameterArray]): ParameterArrayWithFitness = {
    var improvement = nextImprovement(finder, None, cache)
    while (improvement.improved && !isOptimalFitnessReached(improvement.parameters))
      improvement = nextImprovement(finder, Some(improvement), cache)
    improvement.parameters
  }

  private def nextImprovement(finder: ImprovementFinder,
                              last: Option[Improvement],
                              cache: mutable.Set[ParameterArray]): Improvement = {
    val improvement = finder.findIncrementalImprovement(optimizee, last, cache)
    notifyOfChange(improvement.parameters)
    improvement
  }

  private def createImprovementFinder(params: ParameterArrayWithFitness,
                                      baseline: ParameterArray,
                                      useCache: Boolean): ImprovementFinder = {
    params.pa match {
      case _: NumericParameterArray =>
        new NumericImprovementFinder(params, baseline, useCache, msg => trace(msg))
      case _: PermutedParameterArray | _: VariableLengthIntSet =>
        new DiscreteImprovementFinder(params, baseline, msg => trace(msg))
      case _ => throw new IllegalArgumentException("Unexpected params type: " + params.pa.getClass.getName)
    }
  }

  private def isDiscreteProblem(params: ParameterArray): Boolean = params match {
    case _: PermutedParameterArray | _: VariableLengthIntSet => true
    case n: NumericParameterArray => (0 until n.size).exists(i => n.get(i).isIntegerOnly)
    case _ => false
  }
}

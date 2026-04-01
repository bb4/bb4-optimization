// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.strategy.gradient.{DiscreteImprovementFinder, Improvement, ImprovementFinder, NumericImprovementFinder}
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter._
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
    * @param params       the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {

    var currentParams =
      if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
      else ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))

    var numIterations = 1
    log(0, currentParams, 0.0, 0.0, "initial test")
    notifyOfChange(currentParams)

    // Use cache to avoid repeats in discrete spaces; it has little value in continuous numeric spaces.
    val useCache = isDiscreteProblem(currentParams.pa)
    val cache = mutable.HashSet[ParameterArray]()
    if (useCache) cache += currentParams.pa

    // Iterate until there is no significant improvement between iterations.
    // IOW, when the jumpSize is too small (below some threshold).
    val impFinder: ImprovementFinder = createImprovementFinder(currentParams, params, useCache)
    var improvement: Improvement = impFinder.findIncrementalImprovement(optimizee, None, cache)
    currentParams = improvement.parameters

    while (improvement.improved && !isOptimalFitnessReached(currentParams)) {
      //// println(s"iter=$numIterations FITNESS = ${currentParams.fitness} ------------")
      improvement = impFinder.findIncrementalImprovement(optimizee, Some(improvement), cache)
      numIterations += 1
      currentParams = improvement.parameters
      notifyOfChange(currentParams)
    }

    //// println("The optimized parameters after " + numIterations + " iterations are " + currentParams)
    //// println("Last improvement = " + improvement)
    currentParams
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

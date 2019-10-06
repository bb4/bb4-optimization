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

    var numIterations = 0
    log(0, currentParams, 0.0, 0.0, "initial test")
    notifyOfChange(currentParams)

    // Use cache to avoid repeats. This can be a real issue if we have a discrete problem space.
    val cache = mutable.HashSet[ParameterArray]()
    cache += currentParams.pa

    var improvement: Improvement = null

    // Iterate until there is no significant improvement between iterations.
    // IOW, when the jumpSize is too small (below some threshold).
    val impFinder: ImprovementFinder = createImprovementFinder(currentParams)

    do {
      //// println(s"iter=$numIterations FITNESS = ${currentParams.fitness} ------------")
      improvement = impFinder.findIncrementalImprovement(optimizee, improvement, cache)
      numIterations += 1
      currentParams = improvement.parameters
      notifyOfChange(currentParams)
    } while (improvement.improved && !isOptimalFitnessReached(currentParams))

    //// println("The optimized parameters after " + numIterations + " iterations are " + currentParams)
    //// println("Last improvement = " + improvement)
    currentParams
  }

  private def createImprovementFinder(params: ParameterArrayWithFitness): ImprovementFinder = {
    params.pa match {
      case npa: NumericParameterArray => new NumericImprovementFinder(params)
      case dpa @ (_:PermutedParameterArray | _:VariableLengthIntSet) => new DiscreteImprovementFinder(params)
      case _ => throw new IllegalArgumentException("Unexpected params type: " + params.pa.getClass.getName)
    }
  }
}

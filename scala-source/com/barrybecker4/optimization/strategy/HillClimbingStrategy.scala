// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.parameter.improvement.{DiscreteImprovementFinder, Improvement, ImprovementFinder, NumericImprovementFinder}
import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter._

import scala.collection.mutable
import HillClimbingStrategy._


object HillClimbingStrategy {
  /** make steps of this size toward the local maxima, until we need something else. */
  private val INITIAL_JUMP_SIZE = 0.9
  /** continue optimization iteration until the improvement in fitness is less than this.  */
  private val FITNESS_EPS_PERCENT = 0.0000001
  protected val JUMP_SIZE_EPS = 0.000000001
}

/**
  * Hill climbing optimization strategy.
  * Also known as gradient descent. The latter is more accurate in this case because we are trying to
  * find a minimum.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class HillClimbingStrategy(optimizee: Optimizee) extends OptimizationStrategy(optimizee) {

  /** Finds a local minimum.
    * It is a bit like newton's method, but in n dimensions.
    * If we make a jump and find that we are worse off than before, we will backtrack and reduce the stepsize so
    * that we can be guaranteed to improve my some amount on every iteration until the incremental improvement
    * is less than the threshold fitness_eps.
    * @param params       the initial value for the parameters to optimize.
    * @param fitnessRange the approximate absolute value of the fitnessRange.
    * @return the optimized params.
    */
  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    var jumpSize = INITIAL_JUMP_SIZE
    var currentParams =
      if (optimizee.evaluateByComparison) ParameterArrayWithFitness(params, Double.MaxValue)
      else ParameterArrayWithFitness(params, optimizee.evaluateFitness(params))
    var numIterations = 0
    log(0, currentParams, 0.0, 0.0, "initial test")
    notifyOfChange(currentParams)
    val fitnessEps = fitnessRange * FITNESS_EPS_PERCENT / 100.0
    // Use cache to avoid repeats. This can be a real issue if  we have a discrete problem space.
    val cache = mutable.HashSet[ParameterArray]()
    cache += currentParams.pa
    var improvement: Improvement = null
    var improved = false

    // Iterate until there is no significant improvement between iterations.
    // IOW, when the jumpSize is too small (below some threshold).
    val impFinder: ImprovementFinder = createImprovementFinder(currentParams)

    do {
      println(s"iter=$numIterations FITNESS = ${currentParams.fitness} ------------")
      improvement = impFinder.findIncrementalImprovement(optimizee, jumpSize, improvement, cache)
      numIterations += 1
      currentParams = improvement.parameters
      jumpSize = improvement.newJumpSize
      notifyOfChange(currentParams)
      improved = improvement.improvement < -fitnessEps
    } while (improved && (jumpSize > JUMP_SIZE_EPS) && !isOptimalFitnessReached(currentParams))
    println("The optimized parameters after " + numIterations + " iterations are " + currentParams)
    println("Last improvement = " + improvement + " jumpSize=" + jumpSize + " improved=" + improved)
    currentParams
  }

  private def createImprovementFinder(params: ParameterArrayWithFitness): ImprovementFinder = {
    params.pa match {
      case npa: NumericParameterArray => new NumericImprovementFinder(params)
      case dpa @ (_:PermutedParameterArray | _:VariableLengthIntArray) => new DiscreteImprovementFinder(params)
      case _ => throw new IllegalArgumentException("Unexpected params type: " + params.pa.getClass.getName)
    }
  }
}
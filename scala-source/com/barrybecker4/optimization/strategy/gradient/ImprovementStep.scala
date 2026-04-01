// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import scala.collection.mutable

object ImprovementStep {

  /** Increase the size of the radius by this multiplier */
  private val RADIUS_EXPANDER = 1.5

  val JUMP_SIZE_INC_FACTOR = 1.2
  val JUMP_SIZE_DEC_FACTOR = 0.7
}

/**
  * A step in the Hill climbing optimization strategy. Hopefully heads in the right direction.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class ImprovementStep(var optimizee: Optimizee, var iter: ImprovementIteration, var gradLength: Double,
                      var cache: mutable.Set[ParameterArray], var jumpSize: Double, var oldFitness: Double,
                      baselineParams: ParameterArray, useCache: Boolean = true,
                      trace: String => Unit = _ => ()) {

  private var improvement = .0
  private var improved = false

  def getImprovement: Double = improvement

  /** @param params the initial value for the parameters to optimize.
    * @return the parameters to try next.
    */
  def findNextParams(params: ParameterArrayWithFitness): ParameterArrayWithFitness = {
    val maxTries = 100
    var currentParams = findNextCandidateParams(params)
    var numTries = 1
    while (!improved && (jumpSize > JUMP_SIZE_EPS) && numTries < maxTries) {
      currentParams = findNextCandidateParams(currentParams)
      numTries += 1
    }
    currentParams
  }

  /**
    * Consider a nearby neighbor of the passed in params to see if it will yield improvement.
    * @param params parameter to find neighbor of.
    * @return nearby location with better fitness if there is one.
    */
  private def findNextCandidateParams(params: ParameterArrayWithFitness): ParameterArrayWithFitness = {
    var currentParams: NumericParameterArray = requireNumericParameterArray(params.pa)
    val oldParams = params
    iter.updateGradient(jumpSize, gradLength)
    currentParams = currentParams.add(iter.gradient)
    var gaussRadius = 0.01
    var sameParams = false
    if (useCache) {
      // For problems with integer params, avoid retesting the same candidate repeatedly.
      while (cache.contains(currentParams)) {
        sameParams = true
        currentParams = currentParams.getRandomNeighbor(gaussRadius)
        gaussRadius *= ImprovementStep.RADIUS_EXPANDER
      }
      cache += currentParams
    }

    var newParams: ParameterArrayWithFitness = null
    if (optimizee.evaluateByComparison) {
      val deltaFitness = optimizee.compareFitness(currentParams, oldParams.pa)
      val fitness = optimizee.compareFitness(currentParams, baselineParams)
      newParams = ParameterArrayWithFitness(currentParams, fitness)
      improvement = deltaFitness
    }
    else {
      val fitness = optimizee.evaluateFitness(currentParams)
      newParams = ParameterArrayWithFitness(currentParams, fitness)
      improvement = fitness - oldFitness
    }
    improved = improvement < 0
    if (!improved) {
      newParams = oldParams
      if (!sameParams) { // we have not improved, try again with a reduced jump size.
        // This could happen, for example, if we overshot the goal.
        trace("--Warning: the new params are worse, so reduce the step size and try again")
        trace(s"  jumpSize=$jumpSize")
        jumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
      }
    }
    newParams
  }
}

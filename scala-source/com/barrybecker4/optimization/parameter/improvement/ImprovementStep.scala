// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.improvement

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable


object ImprovementStep {
  /** continue optimization iteration until the improvement in fitness is less than this.  */
  protected val JUMP_SIZE_EPS = 0.000000001

  /** Increase the size of the radius by this multiplier */
  private val RADIUS_EXPANDER = 1.5

  val JUMP_SIZE_INC_FACTOR = 1.3
  val JUMP_SIZE_DEC_FACTOR = 0.7
}

/**
  * A step in the Hill climbing optimization strategy. Hopefully heads in the right direction.
  * @param optimizee the thing to be optimized.
  * @author Barry Becker
  */
class ImprovementStep(var optimizee: Optimizee, var iter: ImprovementIteration, var gradLength: Double,
                      var cache: mutable.Set[ParameterArray], var jumpSize: Double, var oldFitness: Double) {

  private var improvement = .0
  private var improved = false

  def getJumpSize: Double = jumpSize
  def getImprovement: Double = improvement

  /** @param params the initial value for the parameters to optimize.
    * @return the parameters to try next.
    */
  def findNextParams(params: NumericParameterArray): NumericParameterArray = {
    var currentParams = params
    val maxTries = 100
    var numTries = 0
    do {
      currentParams = findNextCandidateParams(currentParams)
      numTries += 1
    } while (!improved && (jumpSize > ImprovementStep.JUMP_SIZE_EPS) && numTries < maxTries)
    currentParams
  }

  /**
    * Consider a nearby neighbor of the passed in params to see if it will yield improvement.
    * @param params parameter set to find neighbor of.
    * @return nearby location.
    */
  private def findNextCandidateParams(params: NumericParameterArray) = {
    var currentParams = params
    val oldParams = currentParams.copy
    iter.updateGradient(jumpSize, gradLength)
    //log("gradient = " + iter.gradient + " jumpSize="+ jumpSize);
    currentParams = currentParams.copy
    currentParams.add(iter.gradient)
    var gaussRadius = 0.01
    var sameParams = false
    // for problems with integer params, we want to avoid testing the same candidate over again. */
    while (cache.contains(currentParams)) {
      sameParams = true
      currentParams = currentParams.getRandomNeighbor(gaussRadius)
      gaussRadius *= ImprovementStep.RADIUS_EXPANDER
    }
    cache += currentParams
    if (optimizee.evaluateByComparison) {
      currentParams.setFitness(optimizee.compareFitness(currentParams, oldParams))
      if (currentParams.getFitness < 0) improved = false
      improvement = currentParams.getFitness
    }
    else {
      currentParams.setFitness(optimizee.evaluateFitness(currentParams))
      if (currentParams.getFitness >= oldFitness) improved = false
      improvement = oldFitness - currentParams.getFitness
    }
    improved = improvement > 0
    if (!improved) {
      currentParams = oldParams
      if (!sameParams) { // we have not improved, try again with a reduced jump size.
        //log( "Warning: the new params are worse so reduce the step size and try again");
        //log(numIterations, currentParams.getFitness(), jumpSize, Double.NaN, currentParams, "not improved");
        jumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
      }
    }
    currentParams
  }
}
// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.gradient

import com.barrybecker4.optimization.optimizee.Optimizee
import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}
import scala.collection.mutable

object ImprovementStep {
  /** continue optimization iteration until the improvement in fitness is less than this.  */
  protected val JUMP_SIZE_EPS = 0.000000001

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
                      var cache: mutable.Set[ParameterArray], var jumpSize: Double, var oldFitness: Double) {

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
    while (!improved && (jumpSize > ImprovementStep.JUMP_SIZE_EPS) && numTries < maxTries) {
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
    var currentParams: NumericParameterArray = params.pa.asInstanceOf[NumericParameterArray]
    val oldParams = params
    iter.updateGradient(jumpSize, gradLength)
    //// println(s"gradient = ${iter.gradient}. jumpSize=" + jumpSize)
    currentParams = currentParams.add(iter.gradient)
    var gaussRadius = 0.01
    var sameParams = false
    // for problems with integer params, we want to avoid testing the same candidate over again. */
    while (cache.contains(currentParams)) {
      sameParams = true
      currentParams = currentParams.getRandomNeighbor(gaussRadius)
      gaussRadius *= ImprovementStep.RADIUS_EXPANDER
    }
    cache += currentParams

    var newParams: ParameterArrayWithFitness = null
    if (optimizee.evaluateByComparison) {
      val fitness = optimizee.compareFitness(currentParams, oldParams.pa)
      newParams = ParameterArrayWithFitness(currentParams, fitness)
      if (fitness > 0) improved = false
      improvement = fitness
    }
    else {
      val fitness = optimizee.evaluateFitness(currentParams)
      newParams = ParameterArrayWithFitness(currentParams, fitness)
      if (fitness >= oldFitness) improved = false
      improvement = fitness - oldFitness
    }
    improved = improvement < 0
    if (!improved) {
      newParams = oldParams
      if (!sameParams) { // we have not improved, try again with a reduced jump size.
        // This could happen, for example, if we overshot the goal.
        println( "--Warning: the new params are worse, so reduce the step size and try again")
        println(s"  jumpSize=$jumpSize")
        jumpSize *= ImprovementStep.JUMP_SIZE_DEC_FACTOR
      }
    }
    newParams
  }
}

// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray, ParameterArrayWithFitness}

import scala.util.Random

/**
  * Minimal two-parameter optimizee for [[com.barrybecker4.optimization.viewer.OptimizerEvalApp]]:
  * fitness is Euclidean distance to a fixed target so the 2D viewer shows a non-degenerate path.
  */
class DemoViewerProblem extends OptimizeeProblem {

  private val rnd = new Random(1)
  private val target = (0.4, 0.4)

  override def getName: String = "2D Demo (distance to target)"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val dx = params.get(0).getValue - target._1
    val dy = params.get(1).getValue - target._2
    Math.sqrt(dx * dx + dy * dy)
  }

  override def getFitnessRange: Double = 1.5

  override def getExactSolution: ParameterArrayWithFitness = ParameterArrayWithFitness(
    new NumericParameterArray(
      Array(target._1, target._2),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("p1", "p2"),
      rnd
    ),
    0.0
  )

  override def getInitialGuess: ParameterArray =
    new NumericParameterArray(
      Array(0.5, 0.5),
      Array(0.0, 0.0),
      Array(1.0, 1.0),
      Array("p1", "p2"),
      rnd
    )
}

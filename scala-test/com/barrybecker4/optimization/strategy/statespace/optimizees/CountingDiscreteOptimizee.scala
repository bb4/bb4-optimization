package com.barrybecker4.optimization.strategy.statespace.optimizees

import com.barrybecker4.optimization.optimizee.{DiscreteStateSpace, Optimizee}
import com.barrybecker4.optimization.parameter.ParameterArray

import scala.collection.mutable

/** Wraps a discrete optimizee and records evaluateFitness keys / counts. */
final class CountingDiscreteOptimizee(delegate: Optimizee & DiscreteStateSpace)
  extends Optimizee with DiscreteStateSpace {

  val evaluatedKeys: mutable.Set[String] = mutable.LinkedHashSet.empty
  var evaluateCount: Int = 0

  override def getName: String = "counting-" + delegate.getName
  override def evaluateByComparison: Boolean = delegate.evaluateByComparison
  override def getOptimalFitness: Double = delegate.getOptimalFitness

  override def evaluateFitness(params: ParameterArray): Double = {
    evaluateCount += 1
    evaluatedKeys += params.toCSVString
    delegate.evaluateFitness(params)
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    delegate.compareFitness(params1, params2)

  override def successors(state: ParameterArray): Seq[ParameterArray] = delegate.successors(state)

  override def heuristic(state: ParameterArray): Double = delegate.heuristic(state)
}

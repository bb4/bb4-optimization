// Copyright by Barry G. Becker, 2025-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{DiscreteStateSpace, Optimizee}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable

object StateSpaceSearchStrategy {
  /** Default cap on expanded nodes to avoid runaway memory on large graphs. */
  private val DEFAULT_MAX_EXPANSIONS = 100000
}

/**
  * Best-first search on a discrete graph defined by [[DiscreteStateSpace.successors]].
  * Intended for small or structured state spaces; not a general-purpose A* implementation.
  */
class StateSpaceSearchStrategy(optimizee: Optimizee)
  extends OptimizationStrategy(optimizee) {

  private val stateSpace: DiscreteStateSpace = optimizee match {
    case d: DiscreteStateSpace => d
    case _ =>
      throw new IllegalArgumentException(
        "STATE_SPACE_SEARCH requires an Optimizee that mixes in DiscreteStateSpace")
  }

  private var maxExpansions: Int = StateSpaceSearchStrategy.DEFAULT_MAX_EXPANSIONS

  /** Upper bound on how many states to expand (enqueue successors for). */
  def setMaxExpansions(n: Int): Unit = {
    assert(n > 0)
    maxExpansions = n
  }

  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    val baseline = params
    val initialFitness =
      if (optimizee.evaluateByComparison) optimizee.compareFitness(baseline, baseline)
      else optimizee.evaluateFitness(baseline)
    var best = ParameterArrayWithFitness(baseline, initialFitness)
    notifyOfChange(best)

    implicit val ord: Ordering[ParameterArrayWithFitness] = Ordering.by((p: ParameterArrayWithFitness) => -p.fitness)
    val pq = mutable.PriorityQueue.empty[ParameterArrayWithFitness]
    val visited = mutable.HashSet[ParameterArray]()

    pq.enqueue(best)
    visited += baseline

    var expansions = 0
    while (pq.nonEmpty && expansions < maxExpansions && !isOptimalFitnessReached(best)) {
      val current = pq.dequeue()
      if (current.fitness < best.fitness) {
        best = current
        notifyOfChange(best)
      }
      for (next <- stateSpace.successors(current.pa)) {
        if (!visited.contains(next)) {
          visited += next
          val fit =
            if (optimizee.evaluateByComparison) optimizee.compareFitness(next, baseline)
            else optimizee.evaluateFitness(next)
          val nextWf = ParameterArrayWithFitness(next, fit)
          pq.enqueue(nextWf)
          if (fit < best.fitness) {
            best = nextWf
            notifyOfChange(best)
          }
        }
      }
      expansions += 1
    }

    best
  }
}

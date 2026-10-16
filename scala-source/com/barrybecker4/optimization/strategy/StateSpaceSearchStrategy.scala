// Copyright by Barry G. Becker, 2025-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy

import com.barrybecker4.optimization.optimizee.{
  DiscreteStateSpace, EvaluationBudgetExceededException, Optimizee
}
import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

import scala.collection.mutable

object StateSpaceSearchStrategy {

  /** Canonical key for visited-set dedup (`ParameterArray` often lacks value equality). */
  private[strategy] def stateKey(pa: ParameterArray): String = pa.toCSVString
}

/**
  * Best-first (or optional A*) search on a discrete graph: expands states using
  * [[com.barrybecker4.optimization.optimizee.DiscreteStateSpace]] and its `successors` method.
  * Intended for small or structured state spaces with low branching factor.
  *
  * Visited states are keyed by [[stateKey]] (`toCSVString`) so newly allocated successors that
  * represent the same logical state are not re-evaluated.
  *
  * Budget: catches [[EvaluationBudgetExceededException]] and returns best-so-far
  * (seeded via [[notifyOfChange]] after the initial evaluation).
  *
  * A*: when [[StateSpaceSearchMode.AStar]] is selected and fitness is absolute,
  * frontier priority is `fitness + DiscreteStateSpace.heuristic(state)`. Heuristic must be
  * admissible for A* optimality guarantees.
  */
class StateSpaceSearchStrategy(optimizee: Optimizee)
  extends OptimizationStrategy(optimizee) {

  private val stateSpace: DiscreteStateSpace = optimizee match {
    case d: DiscreteStateSpace => d
    case _ =>
      throw new IllegalArgumentException(
        "STATE_SPACE_SEARCH requires an Optimizee that mixes in DiscreteStateSpace")
  }

  private var config: StateSpaceSearchConfig = StateSpaceSearchConfig.default

  /** Expansions performed in the last [[doOptimization]] call (for tests). */
  private[strategy] var lastExpansions: Int = 0

  def getConfig: StateSpaceSearchConfig = config

  def setConfig(c: StateSpaceSearchConfig): Unit = {
    assert(c.maxExpansions > 0, "maxExpansions must be positive")
    config = c
  }

  /** Upper bound on how many states to expand (enqueue successors for). */
  def setMaxExpansions(n: Int): Unit = {
    assert(n > 0)
    config = config.copy(maxExpansions = n)
  }

  override def doOptimization(params: ParameterArray, fitnessRange: Double): ParameterArrayWithFitness = {
    val baseline = params
    val initialFitness =
      try {
        if (optimizee.evaluateByComparison) optimizee.compareFitness(baseline, baseline)
        else optimizee.evaluateFitness(baseline)
      } catch {
        case _: EvaluationBudgetExceededException =>
          lastExpansions = 0
          return ParameterArrayWithFitness(baseline, Double.MaxValue)
      }
    var best = ParameterArrayWithFitness(baseline, initialFitness)
    notifyOfChange(best)

    val useAStar =
      config.searchMode == StateSpaceSearchMode.AStar && !optimizee.evaluateByComparison

    case class FrontierNode(state: ParameterArrayWithFitness, priority: Double)

    // Min-heap by priority (Scala PriorityQueue is max-heap)
    implicit val ord: Ordering[FrontierNode] = Ordering.by((n: FrontierNode) => -n.priority)
    val pq = mutable.PriorityQueue.empty[FrontierNode]
    val visited = mutable.HashSet[String]()

    def priorityOf(wf: ParameterArrayWithFitness): Double =
      if (useAStar) wf.fitness + stateSpace.heuristic(wf.pa)
      else wf.fitness

    pq.enqueue(FrontierNode(best, priorityOf(best)))
    visited += StateSpaceSearchStrategy.stateKey(baseline)

    var expansions = 0
    try {
      while (pq.nonEmpty && expansions < config.maxExpansions && !isOptimalFitnessReached(best)) {
        val current = pq.dequeue().state
        if (current.fitness < best.fitness) {
          best = current
          notifyOfChange(best)
        }
        for (next <- stateSpace.successors(current.pa)) {
          val key = StateSpaceSearchStrategy.stateKey(next)
          if (!visited.contains(key)) {
            visited += key
            val fit =
              if (optimizee.evaluateByComparison) optimizee.compareFitness(next, baseline)
              else optimizee.evaluateFitness(next)
            val nextWf = ParameterArrayWithFitness(next, fit)
            pq.enqueue(FrontierNode(nextWf, priorityOf(nextWf)))
            if (fit < best.fitness) {
              best = nextWf
              notifyOfChange(best)
            }
          }
        }
        expansions += 1
      }
    } catch {
      case _: EvaluationBudgetExceededException =>
        () // return best below
    }

    lastExpansions = expansions
    best
  }
}

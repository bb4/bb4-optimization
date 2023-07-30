package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Item, Problem, Solution}


case class HybridSolver() extends KnapsackSolver {

  private def MAX_PERFECT = 32


  /** A greedy algorithm for filling the knapsack.
    * It sorts them by value/weight, then takes items in-order until the knapsack is full.
    */
  def findItems(problem: Problem): Solution = {
      if (problem.numItems <= MAX_PERFECT) PerfectSolver().findItems(problem)
      else GreedySolver().findItems(problem)
  }
  
}

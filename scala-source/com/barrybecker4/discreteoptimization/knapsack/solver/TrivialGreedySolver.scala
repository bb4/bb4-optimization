package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Problem, Solution}


case class TrivialGreedySolver() extends KnapsackSolver {

  /** A trivial greedy algorithm for filling the knapsack.
    * It takes items in-order until the knapsack is full
    */
  def findItems(problem: Problem): Solution = {
    var totalValue = 0
    var weight = 0
    val taken = new Array[Int](problem.numItems)
    for (item <- problem.items) {
      if (weight + item.weight <= problem.capacity) {
        taken(item.index) = 1
        totalValue += item.value
        weight += item.weight
      }
      else taken(item.index) = 0
    }
    Solution(totalValue, taken)
  }
  
}

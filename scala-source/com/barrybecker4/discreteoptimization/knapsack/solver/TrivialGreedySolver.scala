package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Problem, Solution}


case class TrivialGreedySolver() extends KnapsackSolver {

  /** A trivial greedy algorithm for filling the knapsack.
    * It takes items in-order until the knapsack is full
    */
  def findItems(problem: Problem): Solution = {
    var totalValue = 0
    var weight = 0
    var taken: List[Int] = List()
    for (item <- problem.items) {
      if (weight + item.weight <= problem.capacity) {
        taken :+= 1
        totalValue += item.value
        weight += item.weight
      }
      else taken :+= 0
    }
    Solution(totalValue, taken)
  }
  
}

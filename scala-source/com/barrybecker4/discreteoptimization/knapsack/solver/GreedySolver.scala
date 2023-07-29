package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Item, Problem, Solution}


case class GreedySolver() extends KnapsackSolver {

  /** A greedy algorithm for filling the knapsack.
    * It sorts them by value/weight, then takes items in-order until the knapsack is full.
    */
  def findItems(problem: Problem): Solution = {

    val sortedItems = problem.items.sortWith(_.valuePerWeight > _.valuePerWeight)
    var takenSet: Set[Int] = Set()

    var totalValue = 0
    var weight = 0
    for (item <- sortedItems) {
      if (weight + item.weight <= problem.capacity) {
        takenSet += item.index
        totalValue += item.value
        weight += item.weight
      }
    }

    val taken: Array[Int] = problem.items.map(item => if (takenSet.contains(item.index)) 1 else 0).toArray
    Solution(totalValue, taken)
  }
  
}

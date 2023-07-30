package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Item, Problem, Solution}


case class PerfectSolver() extends KnapsackSolver {


  /** A greedy algorithm for filling the knapsack.
    * It sorts them by value/weight, then takes items in-order until the knapsack is full.
    */
  def findItems(problem: Problem): Solution = {

    if (problem.numItems == 1) {
      val item = problem.items(0)
      if (item.value < problem.capacity) Solution(item.value, List(1), true)
      else Solution(0, List(0), true)
    }
    else {
      val (item, rest) = (problem.items.head, problem.items.tail)
      val candidate1 = findItems(Problem(problem.capacity, rest))
      
      if (item.weight < problem.capacity) {
        val candidate2 = findItems(Problem(problem.capacity - item.weight, rest))
        if (candidate1.totalValue > item.value + candidate2.totalValue)
          Solution(candidate1.totalValue, 0 :: candidate1.taken, true)
        else
          Solution(item.value + candidate2.totalValue, 1 :: candidate2.taken, true)
      }
      else Solution(candidate1.totalValue, 0 :: candidate1.taken, true)
    }
  }
  
}

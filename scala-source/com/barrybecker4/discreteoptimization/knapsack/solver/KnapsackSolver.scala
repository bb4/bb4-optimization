package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.discreteoptimization.knapsack.model.{Problem, Solution}

trait KnapsackSolver {
  def findItems(problem: Problem): Solution
}

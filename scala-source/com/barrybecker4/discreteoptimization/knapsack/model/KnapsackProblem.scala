package com.barrybecker4.discreteoptimization.knapsack.model

case class KnapsackProblem(capacity: Int, items: IndexedSeq[KnapsackItem]) {
  def numItems: Int = items.length
}

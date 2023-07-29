package com.barrybecker4.discreteoptimization.knapsack.model

case class Problem(capacity: Int, items: IndexedSeq[Item]) {
  def numItems: Int = items.length
}

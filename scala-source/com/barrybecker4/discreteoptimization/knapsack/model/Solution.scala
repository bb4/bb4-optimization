package com.barrybecker4.discreteoptimization.knapsack.model

case class Solution(totalValue: Int, taken: Array[Int], perfect: Boolean = false) {

  // Serialize solution in the course's specified output format
  def serialize(): String = {
    var result = totalValue + (if (perfect) " 1\n" else " 0\n")
    for (i <- taken.indices) {
      result += taken(i) + " "
    }
    result += "\n"
    result
  }
}

package com.barrybecker4.discreteoptimization.knapsack.model

case class KnapsackSolution(totalValue: Int, taken: Array[Int]) {

  // Print the solution in the course's specified output format
  def printFormatted(): Unit = {
    println(totalValue + " 0")
    for (i <- taken.indices) {
      print(taken(i) + " ")
    }
    println()
  }
}

package com.barrybecker4.discreteoptimization

import com.barrybecker4.discreteoptimization.knapsack.model.{Item, Problem, ProblemParser, Solution}
import com.barrybecker4.discreteoptimization.knapsack.solver.{GreedySolver, HybridSolver}

import java.io.*
import java.util
import scala.io.Source


/** An implementation of a greedy algorithm to solve the knapsack problem.
  */
object KnapsackApp {

  /** Read the instance, solve it, and print the solution in the standard output
    *
    * awPVV, ./data/ks_30_0, solver.py, Knapsack Problem 1
    * hHYWS, ./data/ks_50_0, solver.py, Knapsack Problem 2
    * JwWnx, ./data/ks_200_0, solver.py, Knapsack Problem 3
    * Z2tMt, ./data/ks_400_0, solver.py, Knapsack Problem 4
    * PUIxa, ./data/ks_1000_0, solver.py, Knapsack Problem 5
    * AKXWc, ./data/ks_10000_0, solver.py, Knapsack Problem 6
    */
  def main(args: Array[String]): Unit = {
    val fileName = getFileName(args)
    if (fileName == null) {
      println("No filename provided!")
      return
    }

    val problem: Problem = ProblemParser().parseProblem(fileName)
    val solution = HybridSolver().findItems(problem)
    println(solution.serialize())
  }

  private def getFileName(args: Array[String]): String = {
    var fileName: String = null
    // get the temp file name
    for (arg <- args) {
      if (arg.startsWith("-file=")) {
        fileName = arg.substring(6)
      }
    }
    fileName
  }
}

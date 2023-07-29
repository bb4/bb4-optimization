package com.barrybecker4.discreteoptimization

import com.barrybecker4.discreteoptimization.knapsack.model.{KnapsackItem, KnapsackProblem, KnapsackSolution}
import com.barrybecker4.discreteoptimization.knapsack.solver.KnapsackTrivialGreedySolver

import java.io.*
import java.util
import scala.io.Source


/** An implementation of a greedy algorithm to solve the knapsack problem.
  */
object KnapsackApp {

  /** Read the instance, solve it, and print the solution in the standard output
    */
  def main(args: Array[String]): Unit = {
    val fileName = getFileName(args)
    if (fileName == null) {
      println("No filename provided!")
      return
    }
    // read the lines out of the file
    val lines: IndexedSeq[String] = Source.fromFile(fileName).getLines.toIndexedSeq
    val problem: KnapsackProblem = parseProblem(lines)
    val solution = KnapsackTrivialGreedySolver(problem).findItems()
    solution.printFormatted()
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

  private def parseProblem(lines: IndexedSeq[String]): KnapsackProblem = {
    // parse the data in the file
    val firstLine = lines(0).split("\\s+")
    val numItems = firstLine(0).toInt
    val capacity = firstLine(1).toInt
    var items = IndexedSeq[KnapsackItem]()
    for (i <- 0 until numItems) {
      val line = lines(i + 1)
      val parts = line.split("\\s+")
      items :+= KnapsackItem(i, parts(0).toInt, parts(1).toInt)
    }
    KnapsackProblem(capacity, items)
  }

}

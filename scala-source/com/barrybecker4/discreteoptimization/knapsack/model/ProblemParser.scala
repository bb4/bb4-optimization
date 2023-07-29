package com.barrybecker4.discreteoptimization.knapsack.model

import com.barrybecker4.discreteoptimization.knapsack.model.{Item, Problem}

import java.io.File
import scala.io.Source

case class ProblemParser() {

  def parseProblem(fileName: String): Problem = 
    parseProblem(Source.fromFile(fileName))

  def parseProblem(file: File): Problem = 
    parseProblem(Source.fromFile(file))
    
  def parseProblem(source: Source): Problem = {
    val problem = parseProblem(source.getLines().toIndexedSeq)
    source.close()
    problem
  }

  // TODO - use lines: Iterator[String] directly
  private def parseProblem(lines: IndexedSeq[String]): Problem = {
    // parse the data in the file
    val firstLine = lines(0).split("\\s+")
    val numItems = firstLine(0).toInt
    val capacity = firstLine(1).toInt
    var items = IndexedSeq[Item]()
    for (i <- 0 until numItems) {
      val line = lines(i + 1)
      val parts = line.split("\\s+")
      items :+= Item(i, parts(0).toInt, parts(1).toInt)
    }
    Problem(capacity, items)
  }

}

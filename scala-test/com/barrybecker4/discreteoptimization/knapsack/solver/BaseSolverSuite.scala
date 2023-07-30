package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.common.testsupport.strip
import com.barrybecker4.discreteoptimization.knapsack.model.{ProblemParser, Solution}
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.net.URL
import scala.io.Source
import BaseSolverSuite.{PREFIX, PARSER}

object BaseSolverSuite {
  val PREFIX = "scala-test/com/barrybecker4/discreteoptimization/knapsack/solver/"
  val PARSER = ProblemParser()
}
class BaseSolverSuite extends AnyFunSuite {
  
  def createSolver(): KnapsackSolver = TrivialGreedySolver()
  
  def verify(problemName: String, expectedResult: String): Unit = {
    val source: Source = Source.fromFile(PREFIX + problemName) // getClass.getClassLoader.getResource("data/ks_30_0")
    val problem = PARSER.parseProblem(source)
    assertResult(expectedResult) {
      createSolver().findItems(problem).serialize()
    }
  }

}

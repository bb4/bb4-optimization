package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.common.testsupport.strip
import com.barrybecker4.discreteoptimization.knapsack.model.{ProblemParser, Solution}
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.net.URL
import scala.io.Source

class GreedySolverSuite extends AnyFunSuite {

  private val PREFIX = "scala-test/com/barrybecker4/discreteoptimization/knapsack/solver/"
  private val parser = ProblemParser()

  test("ks_30_0") {
    val expected = "90000 0\n1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 \n"
    verify("data/ks_30_0", expected)
  }

  test("ks_50_0") {
    val expected = "141956 0\n1 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 1 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 \n"
    verify("data/ks_50_0", expected)
  }

  test("ks_200_0") {
    val expected = "100062 0\n0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 1 0 \n"
    verify("data/ks_200_0", expected)
  }

  def verify(problemName: String, expectedResult: String): Unit = {
    val source: Source = Source.fromFile(PREFIX + problemName) // getClass.getClassLoader.getResource("data/ks_30_0")
    val problem = parser.parseProblem(source)
    assertResult(expectedResult) {
      GreedySolver().findItems(problem).serialize()
    }
  }

}

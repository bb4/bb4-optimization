package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.common.testsupport.strip
import com.barrybecker4.discreteoptimization.knapsack.model.{ProblemParser, Solution}
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.net.URL
import scala.io.Source

class PerfectSolverSuite extends BaseSolverSuite {

  test("ks_4_0") {
    val expected = "18 1\n1 1 0 0 \n"
    verify("data/ks_4_0", expected)
  }

  test("ks_lecture_dp_1") {
    val expected = "9 1\n0 1 1 \n"
    verify("data/ks_lecture_dp_1", expected)
  }

  test("ks_lecture_dp_2") {
    val expected = "39 1\n1 0 1 0 \n"
    verify("data/ks_lecture_dp_2", expected)
  }

  test("ks_19_0") {
    val expected = "22296 1\n0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 1 \n"
    verify("data/ks_19_0", expected)
  }

  test("ks_30_0") {
    val expected = "99798 1\n0 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 \n"
    verify("data/ks_30_0", expected)
  }

  override def createSolver(): KnapsackSolver = PerfectSolver()

}

package com.barrybecker4.discreteoptimization.knapsack.solver

import com.barrybecker4.common.testsupport.strip
import com.barrybecker4.discreteoptimization.knapsack.model.{ProblemParser, Solution}
import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import java.net.URL
import scala.io.Source

class HybridSolverSuite extends BaseSolverSuite {

  test("ks_4_0") {
    val expected = "18 1\n1 1 0 0 \n"
    verify("data/ks_4_0", expected)
  }

  test("ks_19_0") {
    val expected = "22296 1\n0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 1 \n"
    verify("data/ks_19_0", expected)
  }

  test("ks_30_0") {
    val expected = "99798 1\n0 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 \n"
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

  override def createSolver(): KnapsackSolver = HybridSolver()
}

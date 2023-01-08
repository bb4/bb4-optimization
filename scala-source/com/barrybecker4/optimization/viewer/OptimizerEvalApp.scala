package com.barrybecker4.optimization.viewer

import com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem
import com.barrybecker4.optimization.viewer.ui.OptimizerEvalFrame

object OptimizerEvalApp {

  /**
    * Demonstrate with a trivial one parameter problem.
    * Use GraphAnalyticFunctionSolution in test code for more interesting examples.
    */
  def main(args: Array[String]): Unit = {
    val testProblem = new TrivialProblem
    new OptimizerEvalFrame("test/temp.txt", testProblem)
  }

}
package com.barrybecker4.optimization.viewer

import com.barrybecker4.optimization.optimizee.optimizees.{
  DemoViewerProblem,
  DiscreteGrid2DDemoProblem,
  LineGraphDemoProblem,
  WavyPolynomialViewerProblem,
  AckleyProblem,
  RastriginProblem,
  RosenbrockProblem
}
import com.barrybecker4.optimization.viewer.ui.OptimizerEvalFrame

import javax.swing.SwingUtilities

object OptimizerEvalApp {

  /**
   *  Demonstrates how the different optimization algorithms find near optimal solutions for a collection of varying problems like
   *  - trivial 2D distance,
   *  - a wavy polynomial with many local minima,
   *  - discrete 2D grid
   *  - 1D line
   *  - etc
   * For a single problem without the dropdown, use [[com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem]].
   * For more problems from test sources, run `GraphAnalyticFunctionSolution`.
   */
  def main(args: Array[String]): Unit =
    SwingUtilities.invokeLater(() =>
      new OptimizerEvalFrame(
        "test/temp.txt",
        IndexedSeq(
          new DemoViewerProblem,
          new WavyPolynomialViewerProblem,
          new DiscreteGrid2DDemoProblem,
          new LineGraphDemoProblem,
          AckleyProblem(dimensions = 2),
          RastriginProblem(dimensions = 2),
          RosenbrockProblem(dimensions = 2)
        )
      )
    )

}

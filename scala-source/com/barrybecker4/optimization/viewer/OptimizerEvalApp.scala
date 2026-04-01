package com.barrybecker4.optimization.viewer

import com.barrybecker4.optimization.optimizee.optimizees.{DemoViewerProblem, DiscreteGrid2DDemoProblem, LineGraphDemoProblem}
import com.barrybecker4.optimization.viewer.ui.OptimizerEvalFrame

object OptimizerEvalApp {

  /**
    * Three demos: continuous 2D distance, discrete 2D grid (with [[com.barrybecker4.optimization.optimizee.DiscreteStateSpace]]),
    * and 1D line. For a single problem without the dropdown, use [[com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem]].
    * For more problems from test sources, run `GraphAnalyticFunctionSolution`.
    */
  def main(args: Array[String]): Unit = {
    new OptimizerEvalFrame(
      "test/temp.txt",
      IndexedSeq(
        new DemoViewerProblem,
        new DiscreteGrid2DDemoProblem,
        new LineGraphDemoProblem))
  }

}
package com.barrybecker4.optimization.viewer

import com.barrybecker4.optimization.optimizee.optimizees.DemoViewerProblem
import com.barrybecker4.optimization.viewer.ui.OptimizerEvalFrame

object OptimizerEvalApp {

  /**
    * Default: two-parameter demo so the 2D viewer path fills the panel.
    * For a one-parameter example, use [[com.barrybecker4.optimization.optimizee.optimizees.TrivialProblem]].
    * For the full problem dropdown, run `GraphAnalyticFunctionSolution` from test sources.
    */
  def main(args: Array[String]): Unit = {
    new OptimizerEvalFrame("test/temp.txt", new DemoViewerProblem)
  }

}
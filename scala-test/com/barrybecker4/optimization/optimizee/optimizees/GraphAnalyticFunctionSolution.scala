package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.common.util.FileUtil
import com.barrybecker4.optimization.optimizee.optimizees.problems._
import com.barrybecker4.optimization.viewer.OptimizerEvalFrame

/**
  * This is a simple search space to test the optimization package.
  * The function we will try to maximize is one of the AnalyticFunction variants.
  * Shows the solution visually
  *
  * @author Barry Becker
  */
object GraphAnalyticFunctionSolution {

  /** Place to put performance results from the tests */
  private val PERFORMANCE_LOG = FileUtil.getHomeDir + "scala-test/results/poly_optimization.txt"

  /**
    * This finds the solution for the above optimization problem.
    * Shows the path to the solution graphically.
    */
  def main(args: Array[String]): Unit = {
    new OptimizerEvalFrame(PERFORMANCE_LOG, getAllTestProblems)
  }

  /** @return an array of all the test problems to show in a droplist selector. */
  private def getAllTestProblems: IndexedSeq[OptimizeeProblem] = {
    ParabolaMinVariation.VALUES.map(v => new ParabolaMinFunctionProblem(v)) ++
      DominatingSetVariation.VALUES.map(v => new DominatingSetProblem(v)) ++
      Seq(new SevenElevenProblem()) ++
      SubsetSumVariation.VALUES.map(v => new SubsetSumProblem(v)) ++
      TravelingSalesmanVariation.VALUES.map(v => new TravelingSalesmanProblem(v))
  }
}

package com.barrybecker4.optimization.optimizee

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.HILL_CLIMBING
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class BudgetedOptimizeeSuite extends AnyFunSuite {

  test("EvaluationBudgetExceededException after max evaluations") {
    val rnd = new Random(1)
    val problem = new LineGraphTestProblem(target = 7, lo = 0, hi = 20, rnd = rnd)
    val opt = new Optimizer(problem)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 20, "x")), rnd = rnd)
    intercept[EvaluationBudgetExceededException] {
      opt.doOptimization(HILL_CLIMBING, guess, 100.0, rnd, maxEvaluations = Some(1))
    }
  }
}

package com.barrybecker4.optimization.strategy

import com.barrybecker4.math.MathUtil
import com.barrybecker4.optimization.Optimizer
import com.barrybecker4.optimization.optimizee.LineGraphTestProblem
import com.barrybecker4.optimization.parameter.NumericParameterArray
import com.barrybecker4.optimization.parameter.types.IntegerParameter
import com.barrybecker4.optimization.strategy.STATE_SPACE_SEARCH
import org.scalatest.funsuite.AnyFunSuite

import scala.util.Random

class StateSpaceSearchStrategySuite extends AnyFunSuite {

  test("state space search reaches optimal on small line graph") {
    val rnd = new Random(1)
    MathUtil.RANDOM.setSeed(0)
    val problem = new LineGraphTestProblem(target = 7, lo = 0, hi = 20, rnd = rnd)
    val opt = new Optimizer(problem)
    val guess = new NumericParameterArray(IndexedSeq(new IntegerParameter(0, 0, 20, "x")), rnd = rnd)
    val sol = opt.doOptimization(STATE_SPACE_SEARCH, guess, 100.0, rnd)
    assert(sol.fitness == 0.0)
    assert(sol.pa.get(0).getValue.toInt == 7)
  }
}

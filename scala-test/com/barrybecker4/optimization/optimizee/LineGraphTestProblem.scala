package com.barrybecker4.optimization.optimizee

import com.barrybecker4.optimization.parameter.{NumericParameterArray, ParameterArray}
import com.barrybecker4.optimization.parameter.types.IntegerParameter

import scala.util.Random

/**
  * Test helper: 1D integer line; fitness = |pos - target|; successors are ±1 when in range.
  */
class LineGraphTestProblem(val target: Int, val lo: Int, val hi: Int, rnd: Random)
  extends Optimizee with DiscreteStateSpace {

  override def getName: String = "line"

  override def evaluateByComparison: Boolean = false

  override def evaluateFitness(params: ParameterArray): Double = {
    val v = params.get(0).getValue.toInt
    math.abs(v - target).toDouble
  }

  override def compareFitness(params1: ParameterArray, params2: ParameterArray): Double =
    throw new UnsupportedOperationException

  override def getOptimalFitness: Double = 0.0

  private def npa(value: Int): NumericParameterArray = {
    val p = new IntegerParameter(value, lo, hi, "x")
    new NumericParameterArray(IndexedSeq(p), rnd = rnd)
  }

  override def successors(state: ParameterArray): Seq[ParameterArray] = {
    val v = state.get(0).getValue.toInt
    List(v - 1, v + 1).filter(x => x >= lo && x <= hi).map(npa)
  }
}

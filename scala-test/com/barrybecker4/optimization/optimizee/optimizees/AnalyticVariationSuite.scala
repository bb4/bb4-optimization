package com.barrybecker4.optimization.optimizee.optimizees

import com.barrybecker4.optimization.optimizee.optimizees.problems.ParabolaFunctionConsts
import com.barrybecker4.optimization.optimizee.optimizees.problems.ParabolaMinVariation
import com.barrybecker4.optimization.parameter.{NumericParameterArraySuite, ParameterArray}
import AnalyticVariationSuite.TOL
import ParabolaFunctionConsts._
import com.barrybecker4.optimization.optimizee.optimizees.problems.ParabolaMinVariation
import com.barrybecker4.optimization.optimizee.optimizees.problems.ParabolaMinVariation
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.FunSuite


object AnalyticVariationSuite {
  protected val TOL = 0.0000000000001
}

/**
  * Verify that the minimum value of each variation is the same (1001).
  * @author Barry Becker
  */
class AnalyticVariationSuite extends FunSuite {

  implicit val doubleEq: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(TOL)

  test("VariationMaximum") {
    for (variant <- ParabolaMinVariation.VALUES) {
      val param: ParameterArray = NumericParameterArraySuite.createParamArray(P1, P2)
      assert(EXACT_SOLUTION.getFitness === variant.evaluateFitness(param),
        "Unexpected fitness for " + variant.getClass.getName)
    }
  }
}
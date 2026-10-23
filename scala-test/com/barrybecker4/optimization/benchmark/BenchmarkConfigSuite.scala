// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.strategy.OptimizationStrategyType.*
import org.scalatest.funsuite.AnyFunSuite

class BenchmarkConfigSuite extends AnyFunSuite {

  test("the defaults are used when there are no options") {
    val config = BenchmarkConfig.parse(Seq.empty)
    assertResult(BenchmarkConfig.DEFAULT_TIME_LIMIT_MILLIS)(config.timeLimitMillis)
    assertResult(BenchmarkConfig.DEFAULT_NUM_SEEDS)(config.numSeeds)
    assertResult(BenchmarkConfig.DEFAULT_STRATEGIES)(config.strategies)
    assert(config.problemNames.isEmpty && config.outputFile.isEmpty)
  }

  test("each option is read") {
    val config = BenchmarkConfig.parse(Seq("--time-ms=250", "--max-evals=77", "--seeds=3", "--problems=tour-50,subset-sum",
      "--strategies=HILL_CLIMBING,GENETIC_SEARCH", "--out=results.tsv"))
    assertResult((250L, 77L, 3))((config.timeLimitMillis, config.maxEvaluations, config.numSeeds))
    assertResult(Seq("tour-50", "subset-sum"))(config.problemNames)
    assertResult(Seq(HILL_CLIMBING, GENETIC_SEARCH))(config.strategies)
    assertResult(Some("results.tsv"))(config.outputFile)
  }

  test("anything that is not an option is ignored") {
    assertResult(BenchmarkConfig.DEFAULT_NUM_SEEDS)(BenchmarkConfig.parse(Seq("extra", "--unknown")).numSeeds)
  }

  test("an unknown strategy is an error") {
    intercept[IllegalArgumentException](BenchmarkConfig.parse(Seq("--strategies=NOT_A_STRATEGY")))
  }

  test("an unknown problem is an error that lists the choices") {
    val error = intercept[IllegalArgumentException](BenchmarkProblems.named(Seq("nope")))
    assert(error.getMessage.contains("tour-50"))
  }
}

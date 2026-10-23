// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import com.barrybecker4.optimization.optimizee.optimizees.OptimizeeProblem
import com.barrybecker4.optimization.optimizee.optimizees.problems.*

/**
  * A problem to benchmark. `create` makes a fresh problem for the seed, so that repeated runs start from the same place.
  */
case class BenchmarkProblem(name: String, create: Long => OptimizeeProblem)

/** The problems that are benchmarked. The tours of increasing length show how a strategy scales with permutation size. */
object BenchmarkProblems {

  val ALL: Seq[BenchmarkProblem] = Seq(
    BenchmarkProblem("tsp-standard", seed => { resetSharedRandom(seed); new TravelingSalesmanProblem(TSP_STANDARD) }),
    BenchmarkProblem("tsp-us-capitals", seed => { resetSharedRandom(seed); new TravelingSalesmanProblem(TSP_US_CAPITALS) }),
    BenchmarkProblem("tour-50", seed => new RandomTourProblem(50, seed)),
    BenchmarkProblem("tour-200", seed => new RandomTourProblem(200, seed)),
    BenchmarkProblem("subset-sum", seed => new SubsetSumProblem(TYPICAL_SS)),
    BenchmarkProblem("dominating-set", seed => new DominatingSetProblem(TYPICAL_DS))
  )

  def named(names: Seq[String]): Seq[BenchmarkProblem] =
    if (names.isEmpty) ALL else names.map(n => ALL.find(_.name == n).getOrElse(throw new IllegalArgumentException(
      s"Unknown problem '$n'. The problems are ${ALL.map(_.name).mkString(", ")}")))

  /** The TSP variations pick their initial guess from one shared random generator, so make it start the same. */
  private def resetSharedRandom(seed: Long): Unit = TravelingSalesmanVariation.RANDOM.setSeed(seed)
}

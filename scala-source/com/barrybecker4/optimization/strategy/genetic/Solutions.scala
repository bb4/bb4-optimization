// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

/** Tells whether two parameter arrays are the same candidate, as opposed to merely the same reference. */
private[strategy] object Solutions {

  /** True when two parameter arrays represent the same candidate (same type and [[ParameterArray.candidateIdentity]]). */
  def same(a: ParameterArray, b: ParameterArray): Boolean =
    (a eq b) || (a.getClass == b.getClass && a.candidateIdentity == b.candidateIdentity)

  def contains(population: Iterable[ParameterArrayWithFitness], pa: ParameterArray): Boolean =
    population.exists(m => same(m.pa, pa))

  /** Hashable identity of a candidate; two arrays have equal keys exactly when [[same]] holds. */
  final case class Key(kind: Class[?], identity: Any)

  def key(pa: ParameterArray): Key = Key(pa.getClass, pa.candidateIdentity)
}

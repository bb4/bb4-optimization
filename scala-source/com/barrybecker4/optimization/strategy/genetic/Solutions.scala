// Copyright by Barry G. Becker, 2000-2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.strategy.genetic

import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}

/** Tells whether two parameter arrays are the same candidate, as opposed to merely the same reference. */
private[strategy] object Solutions {

  /** True when two parameter arrays represent the same candidate (same type and parameter values). */
  def same(a: ParameterArray, b: ParameterArray): Boolean =
    (a eq b) || (a.getClass == b.getClass && a.size == b.size &&
      (0 until a.size).forall(i => a.get(i).getValue == b.get(i).getValue))

  def contains(population: Iterable[ParameterArrayWithFitness], pa: ParameterArray): Boolean =
    population.exists(m => same(m.pa, pa))

  /** Hashable identity of a candidate; two arrays have equal keys exactly when [[same]] holds. */
  final case class Key(kind: Class[?], values: IndexedSeq[Double])

  def key(pa: ParameterArray): Key =
    // `+ 0.0` turns -0.0 into 0.0, which are the same value for `same` but differ when boxed.
    Key(pa.getClass, (0 until pa.size).map(i => pa.get(i).getValue + 0.0))
}

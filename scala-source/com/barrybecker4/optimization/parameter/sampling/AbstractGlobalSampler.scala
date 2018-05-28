// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.sampling


/**
  * Finds a set of uniformly distributed global samples in a large numeric parameter space.
  * If the number of samples requested is really large, then all possible values will be returned (if possible).
  * @author Barry Becker
  */
abstract class AbstractGlobalSampler[E] extends Iterator[E] {

  /** becomes false when no more samples to iterate through */
  var hasNext = true

  /** counts up to the number of samples as we iterate */
  protected var counter = 0

  /** Approximate number of samples to retrieve.
    * If the problem space is small and requestedNumSamples is large, it may not be possible to return this
    * many unique samples.
    */
  protected var numSamples = 0L

  /** Globally sample the parameter space.
    * @return the next sample.
    */
  override def next: E
}
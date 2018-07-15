// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

import com.barrybecker4.optimization.parameter.{ParameterArray, ParameterArrayWithFitness}


/**
  * Called whenever the optimizer has improved its optimization of the optimizee.
  * @author Barry Becker
  */
trait OptimizationListener {
  def optimizerChanged(params: ParameterArrayWithFitness): Unit
}

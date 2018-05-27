// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

import com.barrybecker4.optimization.parameter1.types.Parameter


/**
  * Implemented by classes that do something when a parameter gets changed.
  * @author Barry Becker
  */
trait ParameterChangeListener {
  def parameterChanged(param: Parameter): Unit
}

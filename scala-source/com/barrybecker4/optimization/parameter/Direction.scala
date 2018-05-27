// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

case class Direction(multiplier: Int)

/**
  * @author Barry Becker
  */
object Direction  {
  val FORWARD = Direction(1)
  val BACKWARD = Direction(-1)
  //val VALUES = Array(FORWARD, BACKWARD)
}

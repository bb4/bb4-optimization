// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter

/**
  * Direction to nudge a parameter when taking an epsilon step.
  * @param multiplier +1 for forward, -1 for backward
  * @author Barry Becker
  */
enum Direction(val multiplier: Int) {
  case FORWARD extends Direction(1)
  case BACKWARD extends Direction(-1)
}

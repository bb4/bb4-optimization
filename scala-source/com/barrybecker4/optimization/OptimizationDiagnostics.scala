// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization

/** Optional diagnostic output for optimization strategies (off by default). */
trait OptimizationDiagnostics {
  def trace(msg: => String): Unit
}

object OptimizationDiagnostics {

  object Silence extends OptimizationDiagnostics {
    override def trace(msg: => String): Unit = ()
  }

  final class Console(verbose: Boolean) extends OptimizationDiagnostics {
    override def trace(msg: => String): Unit = if (verbose) println(msg)
  }
}

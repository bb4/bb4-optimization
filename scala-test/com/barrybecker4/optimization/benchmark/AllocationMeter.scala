// Copyright by Barry G. Becker, 2026. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.benchmark

import java.lang.management.ManagementFactory

/**
  * Measures how many bytes the current thread allocates. Work that other threads do is not counted, so the number
  * is only meaningful for single threaded searches.
  */
object AllocationMeter {

  private val bean = ManagementFactory.getThreadMXBean match {
    case sunBean: com.sun.management.ThreadMXBean if sunBean.isThreadAllocatedMemorySupported => Some(sunBean)
    case _ => None
  }

  /** @return the bytes allocated by the current thread so far, if the JVM can tell. */
  def bytesAllocatedSoFar: Option[Long] = bean.map(_.getThreadAllocatedBytes(Thread.currentThread().getId))
}

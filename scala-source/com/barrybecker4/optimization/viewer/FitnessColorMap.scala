// Copyright by Barry G. Becker, 2000-2018. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer

import java.awt.Color
import com.barrybecker4.ui.util.ColorMap


object FitnessColorMap {
  private val myValues = Array(0.0, 0.1, 1.0, 10.0, 100.0, 1000.0, 10000.0)
  /** base transparency value. */
  private val CM_TRANS = 100
  /** this colormap is used to show a spectrum of colors representing a group's health status. */
  private val myColors = Array(new Color(250, 0, 0, CM_TRANS + 40), // min. start of low range.
    new Color(255, 40, 0, CM_TRANS), new Color(255, 255, 0, CM_TRANS), new Color(50, 200, 0, CM_TRANS), // high values range.
    new Color(0, 255, 0, CM_TRANS), new Color(0, 255, 255, CM_TRANS), new Color(0, 0, 255, CM_TRANS))
}

/**
  * Custom colormap for visualizing how close values are to actual solution in viewer.
  * @author Barry Becker
  */
class FitnessColorMap extends ColorMap(FitnessColorMap.myValues, FitnessColorMap.myColors)

package com.barrybecker4.optimization.viewer1;

import com.barrybecker4.ui.util.ColorMap;

import java.awt.Color;

/**
 * @author Barry Becker
 */
public class FitnessColorMap extends ColorMap {

    private static final double[] myValues = {
             0.0,
              0.1, 1.0, 10.0, 100.0, 1000.0, 10000.0};
    /** base transparency value.*/
    private static final int CM_TRANS = 100;

    /** this colormap is used to show a spectrum of colors representing a group's health status.*/
    private static final Color[] myColors = {
            new Color(250, 0, 0, CM_TRANS + 40),  // min. start of low range.
            new Color(255, 40, 0, CM_TRANS),
            new Color(255, 255, 0, CM_TRANS),
            new Color(50, 200, 0, CM_TRANS),  // high values range.
            new Color(0, 255, 0, CM_TRANS),
            new Color(0, 255, 255, CM_TRANS),
            new Color(0, 0, 255, CM_TRANS)
    };

    /**
     * our own custom colormap for visualizing values in Go.
     */
    public FitnessColorMap()
    {
        super(myValues, myColors);
    }

}

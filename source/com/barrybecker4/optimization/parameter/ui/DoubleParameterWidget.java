// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter.ui;

import com.barrybecker4.optimization.parameter.ParameterChangeListener;
import com.barrybecker4.optimization.parameter.types.Parameter;
import com.barrybecker4.ui.sliders.LabeledSlider;
import com.barrybecker4.ui.sliders.SliderChangeListener;

import java.awt.*;

/**
 *
 * @author Barry Becker
 */
public class DoubleParameterWidget extends ParameterWidget implements SliderChangeListener {

    private LabeledSlider slider_;

    public DoubleParameterWidget(Parameter param, ParameterChangeListener listener) {
        super(param, listener);
    }

    /**
     * Create a ui widget appropriate for the parameter type.
     */
    @Override
    protected void addChildren() {

         slider_ =
                 new LabeledSlider(parameter.getName(), parameter.getValue(),
                                               parameter.getMinValue(), parameter.getMaxValue());
         if (parameter.isIntegerOnly()) {
             slider_.setShowAsInteger(true);
         }
         slider_.addChangeListener(this);
         add(slider_, BorderLayout.CENTER);
    }

    /**
     * @param slider the slider that changed.
     */
    public void sliderChanged(LabeledSlider slider) {
         parameter.setValue(slider.getValue());
         doNotification();
    }

    @Override
    public void refreshInternal() {
        slider_.setValue(parameter.getValue());
    }
}

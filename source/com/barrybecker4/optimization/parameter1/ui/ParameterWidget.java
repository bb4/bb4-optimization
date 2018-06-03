// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.ui;

import com.barrybecker4.optimization.parameter1.ParameterChangeListener;
import com.barrybecker4.optimization.parameter1.types.Parameter;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author Barry Becker
 */
public abstract class ParameterWidget extends JPanel {

    protected ParameterChangeListener changeListener;
    protected Parameter parameter;
    private boolean notificationEnabled = true;
    private static final int MAX_WIDTH = 1000;
    private static final int MIN_WIDTH = 200;

    public ParameterWidget(Parameter param, ParameterChangeListener listener) {
        changeListener = listener;
        parameter = param;

        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(MAX_WIDTH, getMaxHeight()));
        setMinimumSize(new Dimension(MIN_WIDTH, getMaxHeight()));
        addChildren();
    }

    /**
     * Make sure that the UI reflects the current parameter value, in case it has changed underneath
     */
    public void refresh() {
        // temporarily turn of notification to listeners so that we do not update listeners when
        // we modify our own internal state.
        notificationEnabled = false;
        refreshInternal();
        notificationEnabled = true;
    }

    /**
     * Make sure that the UI reflects the current parameter value, in case it has changed underneath
     */
    public abstract void refreshInternal();

    /**
     * Add the components to represent the parameter widget.
     */
    protected abstract void addChildren();

    protected void doNotification() {
        if (notificationEnabled)
            changeListener.parameterChanged(parameter);
    }

    protected int getMaxHeight() {
        return 40;
    }

}
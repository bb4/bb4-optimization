// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.ui;

import com.barrybecker4.optimization.parameter1.ParameterChangeListener;
import com.barrybecker4.optimization.parameter1.types.BooleanParameter;
import com.barrybecker4.optimization.parameter1.types.Parameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 *
 * @author Barry Becker
 */
public class BooleanParameterWidget extends ParameterWidget implements ItemListener {

    private JCheckBox cb;

    public BooleanParameterWidget(Parameter param, ParameterChangeListener listener) {
        super(param, listener);
    }

   /**
     * Create a ui widget appropriate for the parameter type.
     */
    @Override
    protected void addChildren() {

            cb = new JCheckBox();
            cb.setText(parameter.getName());

            BooleanParameter bparam = (BooleanParameter) parameter;
            cb.setSelected((Boolean)bparam.getNaturalValue());
            cb.addItemListener(this);
            add(cb, BorderLayout.CENTER);
    }

     /**
      * Called when a checkbox selection has changed for a BooleanParameter
      * @param e the item event
      */
    public void itemStateChanged(ItemEvent e) {
        parameter.setValue(cb.isSelected()?1:0);
        doNotification();
    }

    @Override
    public void refreshInternal() {
        cb.setSelected((Boolean)parameter.getNaturalValue());
    }

    @Override
    protected int getMaxHeight() {
        return 20;
    }

}

// Copyright by Barry G. Becker, 2000-2011. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.parameter1.ui;

import com.barrybecker4.optimization.parameter1.ParameterChangeListener;
import com.barrybecker4.optimization.parameter1.types.Parameter;
import com.barrybecker4.optimization.parameter1.types.StringParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Discrete set of dropdown values.
 * @author Barry Becker
 */
public class StringParameterWidget extends ParameterWidget implements ActionListener {

    private JComboBox dropdown;

    public StringParameterWidget(Parameter param, ParameterChangeListener listener) {
        super(param, listener);
    }

   /**
     * Create a ui widget appropriate for the parameter type.
     */
    @Override
    protected void addChildren() {

        // create a dropdown
        StringParameter sparam = (StringParameter) parameter;
        dropdown = new JComboBox(sparam.getStringValues().toArray());
        dropdown.setName(parameter.getName());
        dropdown.setMaximumSize(new Dimension(200, 20));
        dropdown.setToolTipText(parameter.getName());
        dropdown.addActionListener(this);
        add(dropdown, BorderLayout.CENTER);
    }

     /**
      * Called when a ComboBox selection has changed.
      * @param e the item event
      */
    public void actionPerformed(ActionEvent e) {
        parameter.setValue(dropdown.getSelectedIndex());
        doNotification();
    }

    @Override
    public void refreshInternal() {
        dropdown.setSelectedItem(parameter.getNaturalValue());
    }

    @Override
    protected int getMaxHeight() {
        return 20;
    }

}

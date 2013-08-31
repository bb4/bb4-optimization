// Copyright by Barry G. Becker, 2013. Licensed under MIT License: http://www.opensource.org/licenses/MIT
package com.barrybecker4.optimization.viewer;

import com.barrybecker4.ui.components.GradientButton;
import com.barrybecker4.ui.components.TexturedToolBar;
import com.barrybecker4.ui.util.GUIUtil;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Toolbar that appears a the top of the application window.
 * @author Barry Becker
 */
public class NavigationBar extends TexturedToolBar  {

    private static final String IMAGE_PATH = "com/barrybecker4/optimization/viewer/images/";
    protected static final String DIR = IMAGE_PATH;
    private static final ImageIcon zoomInImage = GUIUtil.getIcon(DIR + "zoomIn.png");
    private static final ImageIcon zoomOutImage = GUIUtil.getIcon(DIR + "zoomOut.png");

    private GradientButton zoomInButton_;
    private GradientButton zoomOutButton_;

    private OptimizationViewable navListener;


    /**
     * Button clicks get translated to OptimizationViewable api calls.
     * @param listener the thing that will consume the navigation events for panning and zooming.
     */
    public NavigationBar(OptimizationViewable listener) {

        super(GUIUtil.getIcon(IMAGE_PATH + "ocean_trans_10.png"));
        setListener(new NavBarListener());
        navListener = listener;
        init();
    }

    private void init() {

        zoomInButton_ = createToolBarButton("", "Zoom in", zoomInImage );
        zoomOutButton_ = createToolBarButton("", "Zoom out", zoomOutImage );

        add( Box.createHorizontalStrut(10));
        add( zoomInButton_ );
        add( zoomOutButton_ );
    }

    /**
     * create a toolbar button.
     */
    @Override
    public GradientButton createToolBarButton( String text, String tooltip, Icon icon ) {
        GradientButton button = super.createToolBarButton(text, tooltip, icon);
        button.setMaximumSize( new Dimension(52, 52));
        return button;
    }

    /** handles the button clicks */
    class NavBarListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            GradientButton src = (GradientButton)  e.getSource();

            if (src == zoomInButton_)  {
                navListener.zoomIn();
            }
            else if (src == zoomOutButton_)  {
                navListener.zoomOut();
            }
        }
    }
}

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.gui.swing.action;

import java.awt.event.ActionEvent;
import org.geotools.gui.swing.JMapPane;
import org.geotools.gui.swing.tool.CursorTool;


/**
 * An action for connect a control (probably a JButton) to
 * the JMapPane.reset() method which sets the bounds of the
 * map area to include the full extent of all map layers
 * 
 * @author Michael Bedward
 * @since 2.6
 * @source $URL$
 * @version $Id$
 */
public class ResetAction extends MapAction {
    private static final long serialVersionUID = -4833407589496173672L;
    
    public static final String TOOL_NAME = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/widget").getString("tool_name_reset");
    public static final String TOOL_TIP = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/widget").getString("tool_tip_reset");
    public static final String ICON_IMAGE_LARGE = "/org/geotools/gui/swing/images/reset_32.png";
    public static final String ICON_IMAGE_SMALL = "/org/geotools/gui/swing/images/reset_24.png";
    
    /**
     * Constructor - when used with a JButton the button will
     * display a small icon only
     * 
     * @param pane the map pane being serviced by this action
     */
    public ResetAction(JMapPane pane) {
        this(pane, CursorTool.SMALL_ICON, false);
    }

    /**
     * Constructor
     * 
     * @param pane the map pane being serviced by this action
     * @param toolIcon specifies which, if any, icon the control (e.g. JButton)
     * will display; one of CursorTool.NO_ICON, CursorTool.SMALL_ICON or
     * CursorTool.LARGE_ICON.
     * @param showToolName set to true for the control to display the tool name
     */
    public ResetAction(JMapPane pane, int toolIcon, boolean showToolName) {
        String toolName = showToolName ? TOOL_NAME : null;
        
        String iconImagePath = null;
        switch (toolIcon) {
            case CursorTool.LARGE_ICON:
                iconImagePath = ICON_IMAGE_LARGE;
                break;
                
            case CursorTool.SMALL_ICON:
                iconImagePath = ICON_IMAGE_SMALL;
                break;
        }
        
        super.init(pane, toolName, TOOL_TIP, iconImagePath);
    }
    
    /**
     * Called when the control is activated. Calls the map pane to reset the 
     * display 
     */
    public void actionPerformed(ActionEvent e) {
        pane.reset();
    }

}

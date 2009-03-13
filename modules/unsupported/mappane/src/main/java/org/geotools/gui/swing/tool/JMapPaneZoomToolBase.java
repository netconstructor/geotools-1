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

package org.geotools.gui.swing.tool;

import org.geotools.gui.swing.JMapPane;


/**
 * Abstract base class for the zoom-in and zoom-out tools. Provides getter / setter
 * methods for the zoom increment.
 * 
 * @author Michael Bedward
 */

public abstract class JMapPaneZoomToolBase extends JMapPaneCursorTool {
    public static final double DEFAULT_ZOOM_FACTOR = 1.5;
    
    protected JMapPane pane;
    protected double zoom;

    /**
     * Constructor
     */
    public JMapPaneZoomToolBase() {
        setZoom(DEFAULT_ZOOM_FACTOR);
    }
    
    /**
     * Set the map pane that this tool is servicing
     * 
     * @param pane the instance of JMapPane
     * @throws IllegalArgumentException if pane is null
     */
    public void setMapPane(JMapPane pane) {
        if (pane == null) {
            throw new IllegalArgumentException("the JMapPane instance must be non-null");
        }
        this.pane = pane;
    }
    
    /**
     * Get the current areal zoom increment. 
     * 
     * @return the current zoom increment as a double
     */
    public double getZoom() {
        return zoom;
    }
    
    /**
     * Set the zoom increment
     * 
     * @param newZoom the new zoom increment; values &lt;= 1.0
     * will be ignored
     * 
     * @return the previous zoom increment
     */
    public double setZoom(double newZoom) {
        double old = zoom;
        if (newZoom > 1.0d) {
            zoom = newZoom;
        }
        return old;
    }

}
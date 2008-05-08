/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2006, GeoTools Project Managment Committee (PMC)
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

package org.geotools.gui.swing.map.map2d.event;

import java.util.EventObject;

import org.geotools.gui.swing.map.map2d.SelectableMap2D;
import org.geotools.gui.swing.map.map2d.SelectableMap2D.SELECTION_FILTER;
import org.geotools.gui.swing.map.map2d.handler.SelectionHandler;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.gui.swing.map.map2d.NavigableMap2D;
import org.geotools.gui.swing.map.map2d.handler.NavigationHandler;

/**
 * Selection Event generated by a SelectableMap2D
 * @author Johann Sorel
 */
public class Map2DNavigationEvent extends EventObject{

    private final NavigationHandler oldhandler;
    private final NavigationHandler newhandler;
    
    /**
     * create a Map2DNavigationEvent
     * @param map : Map2D source componant
     * @param oldhandler : old NavigationHandler, can't be null
     * @param newhandler : new NavigationHandler, can't be null
     */
    public Map2DNavigationEvent(NavigableMap2D map, NavigationHandler oldhandler, NavigationHandler newhandler){
        super(map);
        this.oldhandler = oldhandler;
        this.newhandler = newhandler;
    }
    
    /**
     * get the new navigationHandler
     * @return NavigationHandler, can't be null
     */
    public NavigationHandler getHandler() {
        return newhandler;
    }
    
    /**
     * get the previous navigationHandler
     * @return NavigationHandler, can't be null
     */
    public NavigationHandler getPreviousHandler() {
        return oldhandler;
    }
    
    
}

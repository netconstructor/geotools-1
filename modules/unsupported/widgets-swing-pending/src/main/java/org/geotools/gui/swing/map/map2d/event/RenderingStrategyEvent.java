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

import com.vividsolutions.jts.geom.Envelope;
import java.util.EventObject;

import org.geotools.gui.swing.map.map2d.strategy.RenderingStrategy;
import org.geotools.map.MapContext;

/**
 * RenderingStrategy Event generated by a RenderingStrategy
 * @author Johann Sorel
 */
public class RenderingStrategyEvent extends EventObject{

    private final MapContext oldContext ;
    private final MapContext newContext ;
    private final Envelope oldEnvelope ;
    private final Envelope newEnvelope ;
    
    
    /**
     * create a RenderingStrategyEvent
     * @param strategy : Rendering strategy
     * @param oldContext : previous MapContext
     * @param newContext : new MapContext
     * @param maparea : Envelope
     */
    public RenderingStrategyEvent(RenderingStrategy strategy, MapContext oldContext, MapContext newContext, Envelope maparea){
        super(strategy);
        this.oldContext = oldContext;
        this.newContext = newContext;
        this.oldEnvelope = maparea;
        this.newEnvelope = maparea;
    }
    
    /**
     * create a RenderingStrategyEvent
     * @param strategy : Rendering strategy
     * @param context : MapContext
     * @param oldmaparea : previous maparea
     * @param newmaparea : new maparea
     */
    public RenderingStrategyEvent(RenderingStrategy strategy, MapContext context, Envelope oldmaparea, Envelope newmaparea){
        super(strategy);
        this.oldContext = context;
        this.newContext = context;
        this.oldEnvelope = oldmaparea;
        this.newEnvelope = newmaparea;
    }

    /**
     * get previous MapContext
     * @return MapContext , can't be null
     */
    public MapContext getPreviousContext() {
        return oldContext;
    }

    /**
     * get new MapContext
     * @return MapContext , can't be null
     */
    public MapContext getContext() {
        return newContext;
    }
    
    /**
     * get the previous MapArea
     * @return JTS Envelope, can't be null
     */
    public Envelope getPreviousMapArea() {
        return oldEnvelope;
    }

    /**
     * get the new MapArea
     * @return JTS Envelope, can't be null
     */
    public Envelope getMapArea() {
        return newEnvelope;
    }
    
    
    
    
}

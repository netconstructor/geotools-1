/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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
 *
 */
package org.geotools.map;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.map.event.MapLayerListener;
import org.geotools.styling.Style;


/**
 * A layer to be rendered on a device. A layer is an aggregation of both a
 * {@link FeatureCollection}, a {@link Style} and, optionally, a {@link Query}
 *
 * @author Cameron Shorter
 * @author Martin Desruisseaux
 * @version $Id: MapLayer.java,v 1.3 2004/04/18 03:23:03 groldan Exp $
 */
public interface MapLayer {
    /**
     * Get the feature collection for this layer.  If features has not been set
     * yet, then null is returned.
     *
     * @return the features for this layer.
     */
    FeatureSource getFeatureSource();

    /**
     * Get the style for this layer.  If style has not been set, then null is
     * returned.
     *
     * @return The style (SLD).
     */
    Style getStyle();

    /**
     * Sets the style for this layer. If a style has not been defined a default
     * one is used.
     *
     * @param style The new style
     */
    void setStyle(Style style);

    /**
     * Get the title of this layer. If title has not been defined then an empty
     * string is returned.
     *
     * @return The title of this layer.
     */
    String getTitle();

    /**
     * Set the title of this layer. A {@link LayerEvent} is fired if the new
     * title is different from the previous one.
     *
     * @param title The title of this layer.
     */
    void setTitle(String title);

    /**
     * Determine whether this layer is visible on a map pane or whether the
     * layer is hidden.
     *
     * @return <code>true</code> if the layer is visible, or <code>false</code>
     *         if the layer is hidden.
     */
    boolean isVisible();

    /**
     * Specify whether this layer is visible on a map pane or whether the layer
     * is hidden. A {@link LayerEvent} is fired if the visibility changed.
     *
     * @param visible Show the layer if <code>true</code>, or hide the layer if
     *        <code>false</code>
     */
    void setVisible(boolean visible);

    /**
     * Returns the definition query (filter) for this layer. If no definition
     * query has  been defined {@link Query.ALL} is returned.
     *
     * @return
     */
    Query getQuery();

    /**
     * Sets a definition query for the layer wich acts as a filter for the
     * features that the layer will draw.
     * 
     * <p>
     * A consumer must ensure that this query is used in  combination with the
     * bounding box filter generated on each map interaction to limit the
     * number of features returned to those that complains both the definition
     * query  and relies inside the area of interest.
     * </p>
     * <p>
     * IMPORTANT: only include attribute names in the query if you want them to
     * be ALWAYS returned. It is desirable to not include attributes at all
     * but let the layer user (a renderer?) to decide wich attributes are actually
     * needed to perform its requiered operation.
     * </p>
     *
     * @param query
     */
    void setQuery(Query query);

    /**
     * Add a listener to notify when a layer property changes. Changes include
     * layer visibility and the title text.
     *
     * @param listener The listener to add to the listener list.
     */
    void addMapLayerListener(MapLayerListener listener);

    /**
     * Removes a listener from the listener list for this layer.
     *
     * @param listener The listener to remove from the listener list.
     */
    void removeMapLayerListener(MapLayerListener listener);
}

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
package org.geotools.map.events;


/**
 * Legacy event.
 *
 * @author Cameron Shorter
 * @version $Id: BoundingBoxEvent.java,v 1.4 2003/08/18 16:32:31 desruisseaux Exp $
 *
 * @deprecated Use {@link org.geotools.map.event.BoundingBoxEvent} instead.
 */
public class BoundingBoxEvent extends org.geotools.map.event.BoundingBoxEvent {
    /**
     * Constructs a new event.
     *
     * @param source The event source.
     */
    public BoundingBoxEvent(final Object source) {
        super(source);
    }
}

/*$************************************************************************************************
 **
 ** $Id: SpatialTemporalExtent.java 1264 2008-07-09 17:46:15Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/extent/SpatialTemporalExtent.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.extent;

import java.util.Collection;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Extent with respect to date/time and spatial boundaries.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 */
@UML(identifier="EX_SpatialTemporalExtent", specification=ISO_19115)
public interface SpatialTemporalExtent extends TemporalExtent {
    /**
     * Returns the spatial extent component of composite
     * spatial and temporal extent.
     *
     * @return The list of geographic extents (never {@code null}).
     */
    @UML(identifier="spatialExtent", obligation=MANDATORY, specification=ISO_19115)
    Collection<? extends GeographicExtent> getSpatialExtent();
}
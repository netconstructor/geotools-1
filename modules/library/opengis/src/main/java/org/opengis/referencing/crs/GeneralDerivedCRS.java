/*$************************************************************************************************
 **
 ** $Id: GeneralDerivedCRS.java 1265 2008-07-09 18:24:37Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M1/geoapi/src/main/java/org/opengis/referencing/crs/GeneralDerivedCRS.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.referencing.crs;

import org.opengis.referencing.operation.Conversion;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * A coordinate reference system that is defined by its coordinate
 * {@linkplain Conversion conversion} from another coordinate reference system
 * (not by a {@linkplain org.opengis.referencing.datum.Datum datum}).
 *
 * @version <A HREF="http://portal.opengeospatial.org/files/?artifact_id=6716">Abstract specification 2.0</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 */
@UML(identifier="SC_GeneralDerivedCRS", specification=ISO_19111)
public interface GeneralDerivedCRS extends SingleCRS {
    /**
     * Returns the base coordinate reference system.
     *
     * @return The base coordinate reference system.
     */
    @UML(identifier="baseCRS", obligation=MANDATORY, specification=ISO_19111)
    CoordinateReferenceSystem getBaseCRS();

    /**
     * Returns the conversion from the {@linkplain #getBaseCRS base CRS} to this CRS.
     *
     * @return The conversion from the base CRS.
     *
     * @rename {@code definedByConversion} may be a precise description of the association,
     *         but may be confusing as a method name since it doesn't said which CRS is the
     *         source or which one is the target. OGC document 01-009 used {@code toBase()}
     *         method name. By analogy with 01-009, this new interface specifies a method
     *         name which contains the {@code FromBase} words.
     */
    @UML(identifier="definedByConversion", obligation=MANDATORY, specification=ISO_19111)
    Conversion getConversionFromBase();
}
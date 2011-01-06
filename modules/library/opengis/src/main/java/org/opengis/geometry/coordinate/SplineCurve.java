/*$************************************************************************************************
 **
 ** $Id: SplineCurve.java 1352 2009-02-18 20:46:17Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/geometry/coordinate/SplineCurve.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.geometry.coordinate;

import java.util.List;
import org.opengis.geometry.primitive.CurveSegment;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Root for subtypes of {@linkplain CurveSegment curve segment} using some version of spline,
 * either polynomial or rational functions.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as">ISO 19107</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 2.0
 */
@UML(identifier="GM_SplineCurve", specification=ISO_19107)
public interface SplineCurve extends CurveSegment {
    /**
     * The sequence of distinct knots used to define the spline basis functions.
     * Recall that the knot data type holds information on knot multiplicity.
     */
    @UML(identifier="knot", obligation=MANDATORY, specification=ISO_19107)
    List<Knot> getKnots();

    /**
     * The degree of the polynomial used for interpolation in a
     * {@linkplain PolynomialSpline polynomial spline}.
     */
    @UML(identifier="degree", obligation=MANDATORY, specification=ISO_19107)
    int getDegree();

    /**
     * An array of points that are used in the interpolation in this spline curve.
     */
    @UML(identifier="controlPoints", obligation=MANDATORY, specification=ISO_19107)
    PointArray getControlPoints();
}
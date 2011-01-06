/*$************************************************************************************************
 **
 ** $Id: JulianDate.java 982 2007-03-27 10:54:51Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/temporal/JulianDate.java $
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.temporal;

import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * The Julian day numbering system is a temporal coordinate system that has its origin at noon
 * on 1 January 4713 BC in the Julian proleptic calendar. The Julian day number is an integer
 * value; the Julian date is a decimal value that allows greater resolution.
 *
 * @author Stephane Fellah (Image Matters)
 */
@UML(identifier="JulianDate", specification=ISO_19108)
public interface JulianDate extends TemporalCoordinate {
}
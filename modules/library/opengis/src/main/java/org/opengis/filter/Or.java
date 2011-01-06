/*$************************************************************************************************
 **
 ** $Id: Or.java 1154 2007-12-19 22:29:42Z jive $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/filter/Or.java $
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.filter;

// Annotations
import org.opengis.annotation.XmlElement;


/**
 * {@linkplain #evaluate Evaluates} to {@code true} if any of the combined expressions evaluate to {@code true}.
 * <p>
 * This interface exposes no additional methods beyond those of {@link BinaryLogicOperator}.
 * It only serves as a marker of what type of operator this is.
 * </p>
 * <p>
 * You can check if the Or operation is supported using:<pre><code>
 * scalarCapabilities.hasLogicalOperators() == true
 * </code></pre>
 * 
 * @version <A HREF="http://www.opengis.org/docs/02-059.pdf">Implementation specification 1.0</A>
 * @author Chris Dillard (SYS Technologies)
 * @since GeoAPI 2.0
 */
@XmlElement("Or")
public interface Or extends BinaryLogicOperator {
}
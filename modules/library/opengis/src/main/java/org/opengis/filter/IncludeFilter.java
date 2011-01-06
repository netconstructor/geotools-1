/*$************************************************************************************************
 **
 ** $Id: IncludeFilter.java 1133 2007-12-05 14:37:40Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/filter/IncludeFilter.java $
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.filter;

import java.io.ObjectStreamException;
import java.io.Serializable;


/**
 * Indicating "no filtering", evaluates to {@code true}.
 * This is a placeholder filter intended to be used in data structuring definition.
 * <p>
 * <ul>
 *   <li>INCLUDE or  Filter ==> INCLUDE</li>
 *   <li>INCLUDE and Filter ==> Filter</li>
 *   <li>not INCLUDE ==> EXCLUDE</li>
 * </ul>
 * <p>
 * The above does imply that the OR opperator can short circuit on encountering NONE.
 *
 * @author Jody Garnett (Refractions Research, Inc.)
 * @author Martin Desruisseaux (Geomatys)
 */
public final class IncludeFilter implements Filter, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -8429407144421087160L;

    /**
     * Not extensible.
     */
    IncludeFilter() {
    }

    /**
     * Accepts a visitor.
     */
    public Object accept(FilterVisitor visitor, Object extraData) {
        return visitor.visit( this, extraData );
    }

    /**
     * Returns {@code true}, content is included.
     */
    public boolean evaluate(Object object) {
        return true;
    }

    /**
     * Returns a string representation of this filter.
     */
    @Override
    public String toString() {
        return "Filter.INCLUDE";
    }

    /**
     * Returns the canonical instance on deserialization.
     */
    private Object readResolve() throws ObjectStreamException {
        return Filter.INCLUDE;
    }
}
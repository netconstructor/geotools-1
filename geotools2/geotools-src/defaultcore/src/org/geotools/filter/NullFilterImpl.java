/*
 *    Geotools - OpenSource mapping toolkit
 *    (C) 2002, Centre for Computational Geography
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *    
 */

package org.geotools.filter;

import org.geotools.data.*;
import org.geotools.feature.*;

/**
 * Defines a null filter, which checks to see if an attribute is null.
 *
 * @version $Id: NullFilterImpl.java,v 1.2 2002/10/24 14:53:17 ianturton Exp $
 * @author Rob Hranac, Vision for New York
 */
public class NullFilterImpl extends AbstractFilterImpl implements NullFilter {

    /** The null check value, which must be an attribute expression. */
    protected Expression nullCheck = null;


    /**
     * Constructor which flags the operator as between.
     */ 
    public NullFilterImpl () {
        filterType = NULL;
    }


    /**
     * Determines whether or not a given feature is 'inside' this filter.
     *
     * @param nullCheck The value of this 
     * @throws IllegalFilterException Filter is illegal.
     */
    public void nullCheckValue(Expression nullCheck)
        throws IllegalFilterException {
        
        if (nullCheck instanceof AttributeExpressionImpl) {
            this.nullCheck = nullCheck;
        }
        else {
            throw new IllegalFilterException("Attempted to add non-attribute expression to a null filter.");
        }
    }
    
    public Expression getNullCheckValue(){
        return nullCheck;
    }

    /**
     * Determines whether or not a given feature is 'inside' this filter.
     *
     * @param feature Specified feature to examine.
     * @return Flag confirming whether or not this feature is inside the filter.
     */
    public boolean contains(Feature feature) {

        if (nullCheck == null) {
            return false;
        }
        else {
            return (nullCheck.getValue(feature) == null);
        }
    }

    /**
     * Returns a string representation of this filter.
     *
     * @return String representation of the null filter.
     */
    public String toString() {
        return "[ " + nullCheck.toString() + " is null ]";
    }
    
    /** 
     * Compares this filter to the specified object.  Returns true 
     * if the passed in object is the same as this filter.  Checks 
     * to make sure the filter types, and the NullCheckValue are
     * the same.
     *
     * @param obj - the object to compare this LikeFilter against.
     * @return true if specified object is equal to this filter; false otherwise.
     */  
    public boolean equals(Object obj) {
	if (obj.getClass() == this.getClass()){
	    NullFilterImpl nullFilter = (NullFilterImpl)obj;
	    return (nullFilter.getFilterType() == this.filterType &&
		    nullFilter.getNullCheckValue().equals(this.nullCheck));
	} else {
	    return false;
	}
    }    
    
}

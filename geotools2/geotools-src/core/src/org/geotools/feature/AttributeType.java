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

package org.geotools.feature;

/**
 * <p>Stores metadata about a single attribute object.  Note that schemas are 
 * also attributes.  All attribute implementations must be immutable.
 * 
 * All attributes must have four properties:<ol>
 * <li>Name: A string that is used to reference the attribute.</li>
 * <li>Occurences: Number of instances of this attribute per <code>Feature</code>.</li> 
 * <li>Type: The expected Java class of this attribute.</li>
 * <li>Position: All schemas are ordered, so they store the associated position
 * of the attribute.</li></ol></p>
 *
 * @version $Id: AttributeType.java,v 1.7 2003/03/14 23:03:29 cholmesny Exp $
 * @author Rob Hranac, VFNY
 */
public interface AttributeType {
        
    /**
     * Sets the position of this attribute in the schema.
     * 
     * @param position Position of attribute.
     * @return Copy of attribute with modified position.
     */
    AttributeType setPosition(int position);

    /**
     * Whether or not this attribute is a schema.
     * 
     * @return True if schema.
     */
    boolean isNested();
    
    /**
     * Gets the name of this attribute.
     * 
     * @return Name.
     */
    String getName();
    
    /**
     * Gets the type of this attribute.
     * 
     * @return Type.
     */
    Class getType();
    
    /**
     * Gets the occurrences of this attribute.
     * 
     * @return Occurrences.
     */
    int getOccurrences();
    
    /**
     * Gets the position of this attribute.
     * 
     * @return Position.
     */
    int getPosition();    

    /**
     * Returns whether nulls are allowed for this attribute.
     *
     * @return true if nulls are permitted, false otherwise.
     */
    boolean isNillable();

    /**
     * Whether the attribute is a geometry.
     *
     * @return true if the attribute's type is a geometry.
     */
    boolean isGeometry();

}

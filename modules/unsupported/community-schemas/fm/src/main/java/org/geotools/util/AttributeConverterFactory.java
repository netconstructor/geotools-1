/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002-2006, GeoTools Project Managment Committee (PMC)
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
 */
package org.geotools.util;

import org.geotools.factory.Hints;
import org.opengis.feature.Attribute;

/**
 * 
 * @author Gabriel Roldan, Axios Engineering
 */
public class AttributeConverterFactory implements ConverterFactory {

    public Converter createConverter(Class source, Class target, Hints hints) {
        if (!(Attribute.class.isAssignableFrom(source))) {
            return null;
        }
        return new Converter() {
            public Object convert(Object source, Class target) throws Exception {
                Attribute att = (Attribute) source;
                Object value = att.get();
                if (value == null) {
                    return null;
                }
                Object convertedValue = Converters.convert(value, target);
                return convertedValue;
            }
        };
    }

}

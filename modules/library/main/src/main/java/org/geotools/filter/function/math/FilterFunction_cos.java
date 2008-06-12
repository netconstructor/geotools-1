package org.geotools.filter.function.math;

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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

//this code is autogenerated - you shouldnt be modifying it!
import org.geotools.filter.FunctionExpressionImpl;

public class FilterFunction_cos extends FunctionExpressionImpl {

    public FilterFunction_cos() {
        super("cos");
    }

    public int getArgCount() {
        return 1;
    }

    public Object evaluate(Object feature) {
        double arg0;

        try { // attempt to get value and perform conversion
            arg0 = ((Number) getExpression(0).evaluate(feature)).doubleValue();
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function cos argument #0 - expected type double");
        }

        return new Double(Math.cos(arg0));
    }
}

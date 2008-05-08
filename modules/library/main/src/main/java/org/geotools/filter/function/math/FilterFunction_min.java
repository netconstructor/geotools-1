package org.geotools.filter.function.math;

/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2005-2006, GeoTools Project Managment Committee (PMC)
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

public class FilterFunction_min extends FunctionExpressionImpl {

    public FilterFunction_min() {
        super("min");
    }

    public int getArgCount() {
        return 2;
    }

    public Object evaluate(Object feature) {
        double arg0;
        double arg1;

        try { // attempt to get value and perform conversion
            arg0 = ((Number) getExpression(0).evaluate(feature)).doubleValue();
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function min argument #0 - expected type int");
        }

        try { // attempt to get value and perform conversion
            arg1 = ((Number) getExpression(1).evaluate(feature)).doubleValue();
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException(
                    "Filter Function problem for function min argument #1 - expected type int");
        }

        return new Double(Math.min(arg0, arg1));
    }
}
